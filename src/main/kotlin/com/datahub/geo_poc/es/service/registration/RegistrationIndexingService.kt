package com.datahub.geo_poc.es.service.registration

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import com.datahub.geo_poc.es.document.registration.RegistrationDocument
import com.datahub.geo_poc.jpa.entity.Registration
import com.datahub.geo_poc.jpa.repository.RegistrationCursorRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

/**
 * Registration ES 인덱싱 서비스
 * 등기 데이터 인덱스
 */
@Service
class RegistrationIndexingService(
    private val registrationRepo: RegistrationCursorRepository,
    private val esClient: ElasticsearchClient,
    private val indexingDispatcher: CoroutineDispatcher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = RegistrationDocument.INDEX_NAME
        const val WORKER_COUNT = 10
        const val BATCH_SIZE = 1000
    }

    /**
     * 전체 재인덱싱
     */
    fun reindex(): Map<String, Any> = runBlocking {
        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, Any>()

        ensureIndexExists()
        log.info("[Registration] ========== 인덱싱 시작 ==========")

        val totalCount = registrationRepo.count()
        log.info("[Registration] 전체 건수: {}", totalCount)

        val boundaries = findIdBoundaries(WORKER_COUNT)
        log.info("[Registration] {} 개 워커로 병렬 처리 시작, boundaries: {}", WORKER_COUNT, boundaries)

        val processedCount = AtomicInteger(0)
        val indexedCount = AtomicInteger(0)

        coroutineScope {
            val jobs = (0 until boundaries.size - 1).map { workerIndex ->
                async(indexingDispatcher) {
                    processPartition(
                        workerIndex = workerIndex,
                        minId = boundaries[workerIndex],
                        maxId = boundaries[workerIndex + 1],
                        totalCount = totalCount,
                        processedCount = processedCount,
                        indexedCount = indexedCount
                    )
                }
            }
            jobs.awaitAll()
        }

        log.info("[Registration] forcemerge 시작 (백그라운드)...")
        try {
            esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
            log.info("[Registration] forcemerge 완료")
        } catch (e: Exception) {
            log.info("[Registration] forcemerge 요청 완료 (ES 백그라운드 처리 중): {}", e.message)
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[Registration] ========== 인덱싱 완료 ==========")
        log.info("[Registration] 처리: {}/{}, 인덱싱: {}, 소요: {}ms",
            processedCount.get(), totalCount, indexedCount.get(), elapsed)

        results["totalCount"] = totalCount
        results["processed"] = processedCount.get()
        results["indexed"] = indexedCount.get()
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
     * forcemerge 실행
     */
    fun forcemerge(): Map<String, Any> {
        log.info("[Registration] forcemerge 시작 (백그라운드)...")
        try {
            esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
            log.info("[Registration] forcemerge 완료")
        } catch (e: Exception) {
            log.info("[Registration] forcemerge 요청 완료 (ES 백그라운드 처리 중): {}", e.message)
        }

        return mapOf(
            "action" to "forcemerge",
            "success" to true
        )
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
            log.info("[Registration] 인덱스 삭제 완료: {}", INDEX_NAME)
            mapOf("deleted" to true, "index" to INDEX_NAME)
        } else {
            log.info("[Registration] 삭제할 인덱스 없음: {}", INDEX_NAME)
            mapOf("deleted" to false, "index" to INDEX_NAME, "reason" to "not exists")
        }
    }

    // ==================== Private: ID 파티션 경계 계산 ====================

    private fun findIdBoundaries(partitions: Int): List<Int> {
        val result = registrationRepo.findMinMaxId()
        val minId = (result[0] as? Number)?.toInt() ?: 0
        val maxId = (result[1] as? Number)?.toInt() ?: 0

        if (minId == 0 && maxId == 0) return listOf(0, 0)

        val range = maxId - minId + 1
        val chunkSize = (range + partitions - 1) / partitions

        val boundaries = mutableListOf<Int>()
        for (i in 0 until partitions) {
            boundaries.add(minId + i * chunkSize)
        }
        boundaries.add(maxId + 1)

        return boundaries
    }

    // ==================== Private: 파티션 처리 ====================

    private suspend fun processPartition(
        workerIndex: Int,
        minId: Int,
        maxId: Int,
        totalCount: Long,
        processedCount: AtomicInteger,
        indexedCount: AtomicInteger
    ) {
        var lastId: Int? = null
        var workerProcessed = 0
        var workerIndexed = 0

        log.info("[Registration] Worker-{} 시작: id {} ~ {}", workerIndex, minId, maxId)

        while (true) {
            val t0 = System.currentTimeMillis()
            val entities = fetchEntities(minId, maxId, lastId)
            val t1 = System.currentTimeMillis()
            if (entities.isEmpty()) break

            log.debug("[Registration] W-{} DB 조회: {}ms ({}건)", workerIndex, t1 - t0, entities.size)

            val docs = entities.map { RegistrationDocument.fromEntity(it) }

            if (docs.isNotEmpty()) {
                val tBulk0 = System.currentTimeMillis()
                bulkIndex(docs)
                val tBulk1 = System.currentTimeMillis()
                log.debug("[Registration] W-{} bulkIndex: {}ms ({}건)", workerIndex, tBulk1 - tBulk0, docs.size)
            }

            lastId = entities.last().id
            workerProcessed += entities.size
            workerIndexed += docs.size

            val globalProcessed = processedCount.addAndGet(entities.size)
            indexedCount.addAndGet(docs.size)

            if (globalProcessed % 50000 < entities.size) {
                val pct = String.format("%.1f", globalProcessed * 100.0 / totalCount)
                log.info("[Registration] 진행: {}/{} ({}%), 인덱싱: {}",
                    globalProcessed, totalCount, pct, indexedCount.get())
            }
        }

        log.info("[Registration] Worker-{} 완료: {} 건 (인덱싱: {})", workerIndex, workerProcessed, workerIndexed)
    }

    private fun fetchEntities(minId: Int, maxId: Int, lastId: Int?): List<Registration> {
        val pageable = PageRequest.of(0, BATCH_SIZE)
        return if (lastId == null) {
            registrationRepo.findByIdRangeFirst(minId, maxId, pageable)
        } else {
            registrationRepo.findByIdRangeCursor(minId, maxId, lastId, pageable)
        }
    }

    // ==================== Private: ES 인덱스 ====================

    private fun ensureIndexExists() {
        val exists = try {
            esClient.indices().exists { e -> e.index(INDEX_NAME) }.value()
        } catch (e: Exception) {
            false
        }

        if (exists) {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            log.info("[Registration] 기존 인덱스 삭제")
        }

        esClient.indices().create { c ->
            c.index(INDEX_NAME)
                .settings { s ->
                    s.numberOfShards("1")
                        .numberOfReplicas("0")
                }
                .mappings { m ->
                    m.properties("id") { p -> p.integer { it } }
                        .properties("originRegistrationId") { p -> p.integer { it } }
                        .properties("realEstateNumber") { p -> p.keyword { it } }
                        .properties("registrationType") { p -> p.keyword { it } }
                        .properties("address") { p -> p.text { it } }
                        .properties("roadAddress") { p -> p.text { it } }
                        .properties("addressDetail") { p -> p.keyword { it } }
                        .properties("pnuId") { p -> p.keyword { it } }
                        .properties("regionCode") { p -> p.keyword { it } }
                        .properties("sidoCode") { p -> p.keyword { k -> k.eagerGlobalOrdinals(true) } }
                        .properties("sggCode") { p -> p.keyword { it } }
                        .properties("umdCode") { p -> p.keyword { it } }
                        .properties("riCode") { p -> p.keyword { it } }
                        .properties("registrationProcess") { p -> p.keyword { it } }
                        .properties("completedAt") { p -> p.date { it } }
                        .properties("property") { p -> p.keyword { it } }
                        .properties("createdAt") { p -> p.date { it } }
                        .properties("createdBy") { p -> p.long_ { it } }
                        .properties("updatedAt") { p -> p.date { it } }
                        .properties("updatedBy") { p -> p.long_ { it } }
                        .properties("isCollateralAndLease") { p -> p.boolean_ { it } }
                        .properties("isSalesList") { p -> p.boolean_ { it } }
                        .properties("isExpunged") { p -> p.boolean_ { it } }
                        .properties("isEventIgnore") { p -> p.boolean_ { it } }
                        .properties("userId") { p -> p.long_ { it } }
                        .properties("isReopen") { p -> p.boolean_ { it } }
                        .properties("geometry") { p -> p.geoShape { it } }
                        .properties("center") { p -> p.geoPoint { it } }
                        .properties("transactionNumber") { p -> p.keyword { it } }
                        .properties("isLatest") { p -> p.boolean_ { it } }
                        .properties("shareScope") { p -> p.keyword { it } }
                        .properties("returnCode") { p -> p.keyword { it } }
                        .properties("refundStatus") { p -> p.keyword { it } }
                        .properties("approvalNumber") { p -> p.keyword { it } }
                        .properties("registerMasterId") { p -> p.long_ { it } }
                        .properties("permanentDocumentType") { p -> p.keyword { it } }
                        .properties("documentNumber") { p -> p.keyword { it } }
                }
        }
        log.info("[Registration] 인덱스 생성: {}", INDEX_NAME)
    }

    private fun bulkIndex(docs: List<RegistrationDocument>) {
        if (docs.isEmpty()) return

        val operations = docs.map { doc ->
            BulkOperation.of { op ->
                op.index { idx ->
                    idx.index(INDEX_NAME)
                        .id(doc.id.toString())
                        .document(doc)
                }
            }
        }

        val request = BulkRequest.Builder()
            .operations(operations)
            .build()

        val response = esClient.bulk(request)
        if (response.errors()) {
            // geometry 파싱 실패한 문서만 geometry=null로 재시도
            val failedIds = response.items()
                .filter { it.error() != null && it.error()?.reason()?.contains("geometry") == true }
                .mapNotNull { it.id() }
                .toSet()

            if (failedIds.isNotEmpty()) {
                val retryDocs = docs.filter { it.id.toString() in failedIds }
                    .map { it.copy(geometry = null) }
                retryBulkIndex(retryDocs)
                log.debug("[Registration] geometry 오류로 {} 건 재시도 (geometry=null)", failedIds.size)
            }

            // geometry 외 에러 로깅
            val otherErrors = response.items()
                .filter { it.error() != null && it.error()?.reason()?.contains("geometry") != true }
            if (otherErrors.isNotEmpty()) {
                log.warn("[Registration bulkIndex] 기타 실패: {}", otherErrors.size)
                otherErrors.firstOrNull()?.let { item ->
                    log.warn("[Registration bulkIndex] 에러 예시: {}", item.error()?.reason())
                }
            }
        }
    }

    private fun retryBulkIndex(docs: List<RegistrationDocument>) {
        if (docs.isEmpty()) return

        val operations = docs.map { doc ->
            BulkOperation.of { op ->
                op.index { idx ->
                    idx.index(INDEX_NAME)
                        .id(doc.id.toString())
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
            log.warn("[Registration retryBulkIndex] 재시도 후에도 실패: {}/{}", failedCount, docs.size)
        }
    }
}
