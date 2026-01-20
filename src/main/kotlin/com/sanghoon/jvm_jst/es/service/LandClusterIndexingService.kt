package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import com.sanghoon.jvm_jst.es.document.LandClusterDocument
import com.sanghoon.jvm_jst.entity.LandCharacteristic
import com.sanghoon.jvm_jst.es.repository.EsLandCharacteristicRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class LandClusterIndexingService(
    private val esLandCharacteristicRepository: EsLandCharacteristicRepository,
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = "land_cluster"
        const val CHUNK_SIZE = 30000
        val SIDO_CODES = listOf("11", "26", "27", "28", "29", "30", "31", "36", "41", "43", "44", "46", "47", "48", "50", "51", "52")
        val PARALLELISM = SIDO_CODES.size // 17개 시도 = 17개 스레드
        val SIDO_NAMES = mapOf(
            "11" to "서울", "26" to "부산", "27" to "대구", "28" to "인천",
            "29" to "광주", "30" to "대전", "31" to "울산", "36" to "세종",
            "41" to "경기", "43" to "충북", "44" to "충남", "46" to "전남",
            "47" to "경북", "48" to "경남", "50" to "제주", "51" to "강원", "52" to "전북"
        )
    }

    // 병렬 처리 제한된 디스패처
    private val indexingDispatcher = Dispatchers.IO.limitedParallelism(PARALLELISM)
    private val scope = CoroutineScope(SupervisorJob() + indexingDispatcher)

    private val totalIndexed = AtomicLong(0)
    @Volatile private var running = false

    /**
     * 인덱싱 (코루틴)
     * @param sidoCodes 시도코드 목록. null이면 전체
     */
    fun reindex(sidoCodes: List<String>? = null): IndexingResult {
        if (running) {
            return IndexingResult(success = false, message = "이미 실행 중")
        }
        running = true
        totalIndexed.set(0)

        // 인덱스 없으면 매핑과 함께 생성
        ensureIndexExists()

        val targetSidos = sidoCodes?.takeIf { it.isNotEmpty() } ?: SIDO_CODES
        log.info("[reindex] 시작 - 대상 시도: {}, parallelism: {}", targetSidos, PARALLELISM)

        scope.launch {
            val startTime = System.currentTimeMillis()
            try {
                coroutineScope {
                    targetSidos.map { sido ->
                        async { processSido(sido) }
                    }.awaitAll()
                }
                val elapsed = System.currentTimeMillis() - startTime
                val elapsedSec = String.format("%.1f", elapsed / 1000.0)
                val elapsedMin = String.format("%.1f", elapsed / 60000.0)
                log.info("[reindex] 완료 - 총 {}건, {}ms / {}s / {}분",
                    totalIndexed.get(), elapsed, elapsedSec, elapsedMin)

                // segment 병합으로 검색 성능 최적화
                log.info("[reindex] forcemerge 시작...")
                val mergeStart = System.currentTimeMillis()
                esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
                val mergeElapsed = System.currentTimeMillis() - mergeStart
                log.info("[reindex] forcemerge 완료 - {}ms / {}s", mergeElapsed, String.format("%.1f", mergeElapsed / 1000.0))
            } catch (e: Exception) {
                log.error("[reindex] 실패", e)
            } finally {
                running = false
            }
        }

        return IndexingResult(success = true, message = "인덱싱 시작됨")
    }

    /**
     * 시도별 처리 (커서 방식)
     */
    private suspend fun processSido(sido: String) = withContext(indexingDispatcher) {
        val sidoName = SIDO_NAMES[sido] ?: sido
        val sidoPrefix = "$sido%"
        val total = esLandCharacteristicRepository.countBySido(sidoPrefix)

        log.info("[processSido] {} ({}) 시작 - 총 {}건", sidoName, sido, total)

        var cursor: String? = null
        var count = 0L

        while (true) {
            val entities = fetchChunk(sidoPrefix, cursor)
            if (entities.isEmpty()) break

            val docs = entities.map { LandClusterDocument.from(it) }
            val payloadMb = (docs.size * 100) / (1024.0 * 1024.0)
            bulkIndex(docs)

            count += entities.size
            totalIndexed.addAndGet(entities.size.toLong())
            cursor = entities.last().pnu

            if (count % 50000 == 0L) {
                log.info("[processSido] {} ({}): {}/{} (~{} MB)", sidoName, sido, count, total, String.format("%.2f", payloadMb))
            }
        }

        log.info("[processSido] {} ({}) 완료: {}/{}", sidoName, sido, count, total)
    }

    /**
     * JPA 커서 조회
     */
    private fun fetchChunk(sidoPrefix: String, cursor: String?): List<LandCharacteristic> {
        val pageable = PageRequest.ofSize(CHUNK_SIZE)
        return if (cursor == null) {
            esLandCharacteristicRepository.findBySidoFirst(sidoPrefix, pageable)
        } else {
            esLandCharacteristicRepository.findBySidoCursor(sidoPrefix, cursor, pageable)
        }
    }

    /**
     * ES 벌크 인덱싱
     */
    private fun bulkIndex(docs: List<LandClusterDocument>) {
        val operations = docs.map { doc ->
            BulkOperation.of { op ->
                op.index { idx ->
                    idx.index(INDEX_NAME)
                        .id(doc.pnu)
                        .document(doc)
                }
            }
        }

        val request = BulkRequest.Builder()
            .operations(operations)
            .build()

        val response = esClient.bulk(request)
        if (response.errors()) {
            val failedCount = response.items().count { it.error() != null }
            log.warn("[bulkIndex] 일부 실패: {}/{}", failedCount, docs.size)
        }
    }

    /**
     * 진행 상태 조회
     */
    fun getStatus(): IndexingStatus {
        return IndexingStatus(running = running, indexed = totalIndexed.get())
    }

    /**
     * 인덱스 전체 삭제 (인덱스 자체 삭제)
     */
    fun deleteAll(): Boolean {
        log.info("[deleteAll] 인덱스 삭제")
        return try {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            log.info("[deleteAll] 삭제 완료")
            true
        } catch (e: Exception) {
            log.warn("[deleteAll] 삭제 실패 (인덱스 없음?)", e)
            false
        }
    }

    /**
     * 인덱스 없으면 매핑과 함께 생성
     */
    private fun ensureIndexExists() {
        val exists = try {
            esClient.indices().exists { e -> e.index(INDEX_NAME) }.value()
        } catch (e: Exception) {
            false
        }

        if (!exists) {
            log.info("[ensureIndexExists] 인덱스 생성 - {}", INDEX_NAME)
            esClient.indices().create { c ->
                c.index(INDEX_NAME)
                    .settings { s ->
                        s.numberOfShards("4")
                            .numberOfReplicas("0")
                    }
                    .mappings { m ->
                        m.properties("location") { p -> p.geoPoint { it } }
                            .properties("sido") { p ->
                                p.keyword { k -> k.eagerGlobalOrdinals(true) }
                            }
                            .properties("pnu") { p -> p.keyword { it } }
                            .properties("jimokCd") { p -> p.keyword { k -> k.eagerGlobalOrdinals(true) } }
                            .properties("jiyukCd1") { p -> p.keyword { k -> k.eagerGlobalOrdinals(true) } }
                            .properties("mainPurpsCd") { p -> p.keyword { k -> k.eagerGlobalOrdinals(true) } }
                    }
            }
            log.info("[ensureIndexExists] 인덱스 생성 완료")
        }
    }
}

data class IndexingResult(
    val success: Boolean,
    val message: String? = null,
    val totalIndexed: Long = 0,
    val elapsedMs: Long = 0
)

data class IndexingStatus(
    val running: Boolean,
    val indexed: Long
)
