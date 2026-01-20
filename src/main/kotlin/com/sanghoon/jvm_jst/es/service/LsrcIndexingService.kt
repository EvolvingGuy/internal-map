package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import com.sanghoon.jvm_jst.es.document.LandStaticRegionClusterDocument
import com.sanghoon.jvm_jst.es.document.LsrcAggData
import com.sanghoon.jvm_jst.rds.repository.LandCharacteristicCursorRepository
import com.sanghoon.jvm_jst.rds.repository.PnuBoundaryRegionRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * LSRC (Land Static Region Cluster) 인덱싱 서비스
 * 고정형 행정구역 클러스터 - 비즈니스 필터 없는 경우 사용
 */
@Service
class LsrcIndexingService(
    private val landCharRepo: LandCharacteristicCursorRepository,
    private val boundaryRegionRepo: PnuBoundaryRegionRepository,
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LandStaticRegionClusterDocument.INDEX_NAME
        const val PARALLELISM = 20
        const val BATCH_SIZE = 3000
        const val BULK_SIZE = 2000
    }

    /**
     * 전체 재인덱싱
     */
    fun reindex(): Map<String, Any> = runBlocking {
        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, Any>()

        // 1. 인덱스 생성
        ensureIndexExists()
        log.info("[LSRC] 인덱스 생성 완료")

        // 2. 전체 필지 수 조회
        val totalCount = landCharRepo.countAll()
        log.info("[LSRC] 전체 필지 수: {}", totalCount)

        // 3. PNU 범위 N등분
        val boundaries = landCharRepo.findPnuBoundaries(PARALLELISM)
        log.info("[LSRC] PNU 경계: {} 개", boundaries.size)

        // 4. 행정구역별 집계 맵 (스레드 세이프)
        val sdAgg = ConcurrentHashMap<String, LsrcAggData>()   // 시도 (2자리)
        val sggAgg = ConcurrentHashMap<String, LsrcAggData>()  // 시군구 (5자리)
        val emdAgg = ConcurrentHashMap<String, LsrcAggData>()  // 읍면동 (8자리)

        // 5. 코루틴으로 병렬 순회
        val processedCounts = ConcurrentHashMap<Int, Long>()

        coroutineScope {
            val jobs = (0 until boundaries.size - 1).map { i ->
                async(Dispatchers.IO) {
                    val minPnu = boundaries[i]
                    val maxPnu = boundaries[i + 1]
                    var lastPnu: String? = null
                    var processed = 0L

                    while (true) {
                        val rows = landCharRepo.findByPnuRangeAfter(minPnu, maxPnu, lastPnu, BATCH_SIZE)
                        if (rows.isEmpty()) break

                        for (row in rows) {
                            val pnu = row.pnu
                            if (pnu.length < 10) continue

                            // 시도 (앞 2자리 + 8자리 0패딩)
                            val sdCode = pnu.substring(0, 2).padEnd(10, '0')
                            sdAgg.computeIfAbsent(sdCode) { LsrcAggData(it) }
                                .add(row.centerLat, row.centerLng)

                            // 시군구 (앞 5자리 + 5자리 0패딩)
                            val sggCode = pnu.substring(0, 5).padEnd(10, '0')
                            sggAgg.computeIfAbsent(sggCode) { LsrcAggData(it) }
                                .add(row.centerLat, row.centerLng)

                            // 읍면동 (앞 8자리 + 2자리 0패딩)
                            val emdCode = pnu.substring(0, 8).padEnd(10, '0')
                            emdAgg.computeIfAbsent(emdCode) { LsrcAggData(it) }
                                .add(row.centerLat, row.centerLng)
                        }

                        lastPnu = rows.last().pnu
                        processed += rows.size

                        if (processed % 30000 == 0L) {
                            log.info("[LSRC] Worker-{} 진행: {} 건 (lastPnu={})", i, processed, lastPnu)
                        }
                    }

                    processedCounts[i] = processed
                    log.info("[LSRC] Worker-{} 완료: {} 건", i, processed)
                }
            }
            jobs.awaitAll()
        }

        val totalProcessed = processedCounts.values.sum()
        log.info("[LSRC] 전체 순회 완료: {} 건", totalProcessed)

        // 6. 행정구역 이름 매핑 (00으로 끝나지 않는 것만)
        val regionNames = boundaryRegionRepo.findAll()
            .filter { !it.regionCode.endsWith("00") }
            .associate { it.regionCode to (it.regionKoreanName ?: "") }
        log.info("[LSRC] 행정구역 이름 로드: {} 개", regionNames.size)

        // 7. ES 배치 인덱싱
        val docs = mutableListOf<LandStaticRegionClusterDocument>()

        // SD
        for ((code, agg) in sdAgg) {
            val name = regionNames[code] ?: regionNames[code.substring(0, 2)] ?: ""
            docs.add(agg.toDocument("SD", name))
        }
        log.info("[LSRC] SD 문서: {} 개", sdAgg.size)

        // SGG
        for ((code, agg) in sggAgg) {
            val name = regionNames[code] ?: regionNames[code.substring(0, 5)] ?: ""
            docs.add(agg.toDocument("SGG", name))
        }
        log.info("[LSRC] SGG 문서: {} 개", sggAgg.size)

        // EMD
        for ((code, agg) in emdAgg) {
            val name = regionNames[code] ?: ""
            docs.add(agg.toDocument("EMD", name))
        }
        log.info("[LSRC] EMD 문서: {} 개", emdAgg.size)

        // 배치 인덱싱
        bulkIndexChunked(docs, BULK_SIZE)
        log.info("[LSRC] ES 인덱싱 완료: {} 개", docs.size)

        // 8. forcemerge
        esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
        log.info("[LSRC] forcemerge 완료")

        val elapsed = System.currentTimeMillis() - startTime

        results["sd"] = sdAgg.size
        results["sgg"] = sggAgg.size
        results["emd"] = emdAgg.size
        results["total"] = docs.size
        results["processed"] = totalProcessed
        results["elapsedMs"] = elapsed
        results["success"] = true

        results
    }

    /**
     * 인덱스 카운트
     */
    fun count(): Long {
        return try {
            esClient.count { c -> c.index(INDEX_NAME) }.count()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 인덱스 삭제
     */
    fun deleteIndex(): Map<String, Any> {
        val exists = try {
            esClient.indices().exists { e -> e.index(INDEX_NAME) }.value()
        } catch (e: Exception) {
            false
        }

        return if (exists) {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            log.info("[LSRC] 인덱스 삭제 완료: {}", INDEX_NAME)
            mapOf("deleted" to true, "index" to INDEX_NAME)
        } else {
            log.info("[LSRC] 삭제할 인덱스 없음: {}", INDEX_NAME)
            mapOf("deleted" to false, "index" to INDEX_NAME, "reason" to "not exists")
        }
    }

    /**
     * forcemerge 실행
     */
    fun forcemerge(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        log.info("[LSRC] forcemerge 시작...")
        esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LSRC] forcemerge 완료: {}ms", elapsed)

        return mapOf(
            "action" to "forcemerge",
            "elapsedMs" to elapsed,
            "success" to true
        )
    }

    private fun ensureIndexExists() {
        val exists = try {
            esClient.indices().exists { e -> e.index(INDEX_NAME) }.value()
        } catch (e: Exception) {
            false
        }

        if (exists) {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            log.info("[LSRC] 기존 인덱스 삭제")
        }

        esClient.indices().create { c ->
            c.index(INDEX_NAME)
                .settings { s ->
                    s.numberOfShards("1")
                        .numberOfReplicas("0")
                }
                .mappings { m ->
                    m.properties("level") { p -> p.keyword { it } }
                        .properties("code") { p -> p.keyword { it } }
                        .properties("name") { p -> p.keyword { it } }
                        .properties("count") { p -> p.integer { it } }
                        .properties("center") { p -> p.geoPoint { it } }
                }
        }
        log.info("[LSRC] 인덱스 생성: {}", INDEX_NAME)
    }

    private fun bulkIndex(docs: List<LandStaticRegionClusterDocument>) {
        if (docs.isEmpty()) return

        val operations = docs.map { doc ->
            BulkOperation.of { op ->
                op.index { idx ->
                    idx.index(INDEX_NAME)
                        .id(doc.id)
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
            log.warn("[LSRC bulkIndex] 일부 실패: {}/{}", failedCount, docs.size)
        }
    }

    private fun bulkIndexChunked(docs: List<LandStaticRegionClusterDocument>, chunkSize: Int) {
        var indexed = 0
        docs.chunked(chunkSize).forEachIndexed { idx, chunk ->
            bulkIndex(chunk)
            indexed += chunk.size
            if ((idx + 1) % 5 == 0) {
                log.info("[LSRC] 청크 진행: {}/{}", indexed, docs.size)
            }
        }
    }
}
