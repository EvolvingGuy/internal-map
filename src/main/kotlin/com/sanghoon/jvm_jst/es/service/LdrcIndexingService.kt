package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import com.sanghoon.jvm_jst.es.document.LandDynamicRegionClusterDocument
import com.sanghoon.jvm_jst.es.document.LdrcAggData
import com.sanghoon.jvm_jst.rds.repository.PnuAggCursorRepository
import com.sanghoon.jvm_jst.rds.repository.PnuBoundaryRegionRepository
import com.uber.h3core.H3Core
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.LongAdder

/**
 * LDRC (Land Dynamic Region Cluster) 인덱싱 서비스
 * EMD -> SGG -> SD 체인 방식
 */
@Service
class LdrcIndexingService(
    private val pnuAggCursorRepo: PnuAggCursorRepository,
    private val boundaryRegionRepo: PnuBoundaryRegionRepository,
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val h3 = H3Core.newInstance()

    companion object {
        const val INDEX_NAME = LandDynamicRegionClusterDocument.INDEX_NAME
        const val PARALLELISM = 20
        const val BATCH_SIZE = 2000
        const val ES_BATCH_SIZE = 5000
    }

    // ==================== Public API ====================

    fun reindex(): Map<String, Any> = runBlocking {
        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, Any>()

        // 1. EMD 인덱싱 (DB → ES)
        results["emd"] = reindexEmdInternal()
        esClient.indices().refresh { r -> r.index(INDEX_NAME) }

        // 2. SGG 인덱싱 (ES EMD → ES SGG)
        results["sgg"] = reindexSggInternal()
        esClient.indices().refresh { r -> r.index(INDEX_NAME) }

        // 3. SD 인덱싱 (ES SGG → ES SD)
        results["sd"] = reindexSdInternal()

        esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }

        results["totalMs"] = System.currentTimeMillis() - startTime
        results["success"] = true
        results
    }

    fun reindexEmd(): Map<String, Any> = runBlocking { reindexEmdInternal() }
    fun reindexSgg(): Map<String, Any> = runBlocking { reindexSggInternal() }
    fun reindexSd(): Map<String, Any> = runBlocking { reindexSdInternal() }

    fun count(): Long = try { esClient.count { c -> c.index(INDEX_NAME) }.count() } catch (e: Exception) { 0 }

    fun forcemerge(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
        return mapOf("action" to "forcemerge", "elapsedMs" to (System.currentTimeMillis() - startTime), "success" to true)
    }

    fun deleteIndex(): Map<String, Any> {
        val exists = try { esClient.indices().exists { it.index(INDEX_NAME) }.value() } catch (_: Exception) { false }
        return if (exists) {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            mapOf("deleted" to true, "index" to INDEX_NAME)
        } else {
            mapOf("deleted" to false, "index" to INDEX_NAME, "reason" to "not exists")
        }
    }

    // ==================== EMD 인덱싱 (DB -> ES) ====================

    private suspend fun reindexEmdInternal(): Map<String, Any> = coroutineScope {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] ========== EMD 인덱싱 시작 ==========")

        ensureIndexExists()

        val maxId = pnuAggCursorRepo.maxIdEmd10()
        log.info("[LDRC] EMD maxId: {}", maxId)

        val chunkSize = (maxId + PARALLELISM - 1) / PARALLELISM
        log.info("[LDRC] EMD {}개 워커로 병렬 처리 시작 (워커당 ~{} 건)", PARALLELISM, chunkSize)

        val indexedCount = LongAdder()
        val processedCount = LongAdder()

        val jobs = (0 until PARALLELISM).map { workerIdx ->
            val workerMinId = workerIdx * chunkSize
            val workerMaxId = minOf((workerIdx + 1) * chunkSize, maxId)
            async(Dispatchers.IO) {
                processEmdPartition(workerIdx, workerMinId, workerMaxId, maxId, processedCount, indexedCount)
            }
        }
        jobs.awaitAll()

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LDRC] EMD 완료: {} 건, {}ms", indexedCount.sum(), elapsed)
        mapOf("level" to "EMD", "indexed" to indexedCount.sum(), "elapsedMs" to elapsed)
    }

    private fun processEmdPartition(
        workerIdx: Int,
        minId: Long,
        maxId: Long,
        totalMaxId: Long,
        processedCount: LongAdder,
        indexedCount: LongAdder
    ) {
        var lastId = minId
        while (true) {
            val rows = pnuAggCursorRepo.findEmd10ByIdRange(lastId, maxId, BATCH_SIZE)
            if (rows.isEmpty()) break

            val docs = rows.map { row ->
                val codeStr = row.code.toString()
                LandDynamicRegionClusterDocument(
                    id = "EMD_${row.code}_${row.h3Index}",
                    level = "EMD",
                    code = codeStr,
                    sdCode = codeStr.take(2),
                    sggCode = codeStr.take(5),
                    h3Index = row.h3Index,
                    count = row.cnt,
                    sumLat = row.sumLat,
                    sumLng = row.sumLng
                )
            }
            bulkIndex(docs)
            lastId = rows.last().id
            indexedCount.add(docs.size.toLong())
            processedCount.add(rows.size.toLong())

            val done = processedCount.sum()
            if (done % 100000 == 0L) {
                log.info("[LDRC] EMD Worker-{} id={} - {}/{}", workerIdx, lastId, done, totalMaxId)
            }
        }
    }

    // ==================== SGG 인덱싱 (ES EMD -> ES SGG) ====================

    private suspend fun reindexSggInternal(): Map<String, Any> = coroutineScope {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] ========== SGG 인덱싱 시작 ==========")

        // 1. boundary_region에서 시군구 코드 목록 조회
        val sggRegions = boundaryRegionRepo.findByGubun("sigungu")
        val sggCodes = sggRegions.map { it.regionCode }
        log.info("[LDRC] SGG 코드 {} 개 조회 완료, {} 워커로 병렬 처리", sggCodes.size, PARALLELISM)

        val indexedCount = LongAdder()
        val processedCount = LongAdder()

        // 2. 코드 목록을 워커 수만큼 청크로 분할하여 병렬 처리
        val chunks = sggCodes.chunked((sggCodes.size + PARALLELISM - 1) / PARALLELISM)
        val jobs = chunks.mapIndexed { workerIdx, chunk ->
            async(Dispatchers.IO) {
                chunk.forEach { fullSggCode ->
                    val sggCode5 = fullSggCode.take(5)
                    val sdCode = fullSggCode.take(2)
                    val emdDocs = fetchBySggCode(sggCode5)

                    if (emdDocs.isNotEmpty()) {
                        val aggMap = mutableMapOf<Long, LdrcAggData>()
                        for (doc in emdDocs) {
                            val parentH3 = h3.cellToParent(doc.h3Index, 7)
                            aggMap.compute(parentH3) { _, existing ->
                                (existing ?: LdrcAggData(parentH3, sggCode5, sdCode, sggCode5)).also {
                                    it.add(doc.count, doc.sumLat, doc.sumLng)
                                }
                            }
                        }

                        val sggDocs = aggMap.values.map { it.toDocument("SGG") }
                        bulkIndex(sggDocs)
                        indexedCount.add(sggDocs.size.toLong())
                    }

                    processedCount.increment()
                    log.info("[LDRC] SGG Worker-{} {}/{} - {} (EMD {} 건)", workerIdx, processedCount.sum(), sggCodes.size, sggCode5, emdDocs.size)
                }
            }
        }
        jobs.awaitAll()

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LDRC] SGG 완료: {} 건, {}ms", indexedCount.sum(), elapsed)
        mapOf("level" to "SGG", "indexed" to indexedCount.sum(), "elapsedMs" to elapsed)
    }

    // ==================== SD 인덱싱 (ES SGG -> ES SD) ====================

    private suspend fun reindexSdInternal(): Map<String, Any> = coroutineScope {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] ========== SD 인덱싱 시작 ==========")

        // 1. boundary_region에서 시도 코드 목록 조회
        val sdRegions = boundaryRegionRepo.findByGubun("sido")
        val sdCodes = sdRegions.map { it.regionCode }
        log.info("[LDRC] SD 코드 {} 개 조회 완료, {} 워커로 병렬 처리", sdCodes.size, PARALLELISM)

        val indexedCount = LongAdder()
        val processedCount = LongAdder()

        // 2. 코드 목록을 워커 수만큼 청크로 분할하여 병렬 처리
        val chunks = sdCodes.chunked((sdCodes.size + PARALLELISM - 1) / PARALLELISM)
        val jobs = chunks.mapIndexed { workerIdx, chunk ->
            async(Dispatchers.IO) {
                chunk.forEach { fullSdCode ->
                    val sdCode2 = fullSdCode.take(2)
                    val sggDocs = fetchBySdCode("SGG", sdCode2)

                    if (sggDocs.isNotEmpty()) {
                        val aggMap = mutableMapOf<Long, LdrcAggData>()
                        for (doc in sggDocs) {
                            val parentH3 = h3.cellToParent(doc.h3Index, 5)
                            aggMap.compute(parentH3) { _, existing ->
                                (existing ?: LdrcAggData(parentH3, sdCode2, sdCode2, "")).also {
                                    it.add(doc.count, doc.sumLat, doc.sumLng)
                                }
                            }
                        }

                        val sdDocs = aggMap.values.map { it.toDocument("SD") }
                        bulkIndex(sdDocs)
                        indexedCount.add(sdDocs.size.toLong())
                    }

                    processedCount.increment()
                    log.info("[LDRC] SD Worker-{} {}/{} - {} (SGG {} 건)", workerIdx, processedCount.sum(), sdCodes.size, sdCode2, sggDocs.size)
                }
            }
        }
        jobs.awaitAll()

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LDRC] SD 완료: {} 건, {}ms", indexedCount.sum(), elapsed)
        mapOf("level" to "SD", "indexed" to indexedCount.sum(), "elapsedMs" to elapsed)
    }

    // ==================== 공통 ====================

    /**
     * sggCode로 EMD 조회 (term 쿼리)
     */
    private fun fetchBySggCode(sggCode: String): List<LandDynamicRegionClusterDocument> {
        val results = mutableListOf<LandDynamicRegionClusterDocument>()

        var scrollId: String? = null
        val scrollResponse = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(ES_BATCH_SIZE)
                .scroll { sc -> sc.time("1m") }
                .query { q ->
                    q.bool { b ->
                        b.must { m -> m.term { t -> t.field("level").value("EMD") } }
                            .must { m -> m.term { t -> t.field("sggCode").value(sggCode) } }
                    }
                }
        }, LandDynamicRegionClusterDocument::class.java)

        scrollId = scrollResponse.scrollId()
        scrollResponse.hits().hits().mapNotNullTo(results) { it.source() }

        while (true) {
            val resp = esClient.scroll({ s ->
                s.scrollId(scrollId).scroll { sc -> sc.time("1m") }
            }, LandDynamicRegionClusterDocument::class.java)

            val hits = resp.hits().hits()
            if (hits.isEmpty()) break

            hits.mapNotNullTo(results) { it.source() }
            scrollId = resp.scrollId()
        }

        if (scrollId != null) {
            try { esClient.clearScroll { c -> c.scrollId(scrollId) } } catch (_: Exception) {}
        }

        return results
    }

    /**
     * sdCode로 조회 (term 쿼리)
     */
    private fun fetchBySdCode(level: String, sdCode: String): List<LandDynamicRegionClusterDocument> {
        val results = mutableListOf<LandDynamicRegionClusterDocument>()

        var scrollId: String? = null
        val scrollResponse = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(ES_BATCH_SIZE)
                .scroll { sc -> sc.time("1m") }
                .query { q ->
                    q.bool { b ->
                        b.must { m -> m.term { t -> t.field("level").value(level) } }
                            .must { m -> m.term { t -> t.field("sdCode").value(sdCode) } }
                    }
                }
        }, LandDynamicRegionClusterDocument::class.java)

        scrollId = scrollResponse.scrollId()
        scrollResponse.hits().hits().mapNotNullTo(results) { it.source() }

        while (true) {
            val resp = esClient.scroll({ s ->
                s.scrollId(scrollId).scroll { sc -> sc.time("1m") }
            }, LandDynamicRegionClusterDocument::class.java)

            val hits = resp.hits().hits()
            if (hits.isEmpty()) break

            hits.mapNotNullTo(results) { it.source() }
            scrollId = resp.scrollId()
        }

        if (scrollId != null) {
            try { esClient.clearScroll { c -> c.scrollId(scrollId) } } catch (_: Exception) {}
        }

        return results
    }

    private fun ensureIndexExists() {
        val exists = try { esClient.indices().exists { it.index(INDEX_NAME) }.value() } catch (_: Exception) { false }

        if (exists) {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
        }

        esClient.indices().create { c ->
            c.index(INDEX_NAME)
                .settings { s -> s.numberOfShards("1").numberOfReplicas("0") }
                .mappings { m ->
                    m.properties("id") { p -> p.keyword { it } }
                        .properties("level") { p -> p.keyword { it } }
                        .properties("code") { p -> p.keyword { it } }
                        .properties("sdCode") { p -> p.keyword { it } }
                        .properties("sggCode") { p -> p.keyword { it } }
                        .properties("h3Index") { p -> p.long_ { it } }
                        .properties("count") { p -> p.integer { it } }
                        .properties("sumLat") { p -> p.double_ { it } }
                        .properties("sumLng") { p -> p.double_ { it } }
                }
        }
        log.info("[LDRC] 인덱스 생성: {}", INDEX_NAME)
    }

    private fun bulkIndex(docs: List<LandDynamicRegionClusterDocument>) {
        if (docs.isEmpty()) return

        val operations = docs.map { doc ->
            BulkOperation.of { op ->
                op.index { idx -> idx.index(INDEX_NAME).id(doc.id).document(doc) }
            }
        }

        val response = esClient.bulk(BulkRequest.Builder().operations(operations).build())
        if (response.errors()) {
            log.warn("[LDRC] bulk 일부 실패: {}/{}", response.items().count { it.error() != null }, docs.size)
        }
    }
}
