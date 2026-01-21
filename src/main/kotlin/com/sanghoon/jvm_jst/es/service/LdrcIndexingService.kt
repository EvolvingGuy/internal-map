package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import com.sanghoon.jvm_jst.es.document.LandDynamicRegionClusterDocument
import com.sanghoon.jvm_jst.es.document.LdrcAggData
import com.sanghoon.jvm_jst.rds.repository.PnuAggCursorRepository
import com.uber.h3core.H3Core
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.LongAdder

/**
 * LDRC (Land Dynamic Region Cluster) 인덱싱 서비스
 * EMD, SGG, SD 모두 r3_pnu_agg_emd_10 테이블에서 독립적으로 인덱싱
 */
@Service
class LdrcIndexingService(
    private val pnuAggCursorRepo: PnuAggCursorRepository,
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

        // 인덱스 생성 (공통)
        ensureIndexExists()

        // 모두 r3_pnu_agg_emd_10 테이블에서 독립적으로 인덱싱
        // 병렬로 실행 가능 (각각 RDB에서 독립적으로 읽음)
        val emdJob = async { reindexEmdInternal() }
        val sggJob = async { reindexSggInternal() }
        val sdJob = async { reindexSdInternal() }

        results["emd"] = emdJob.await()
        results["sgg"] = sggJob.await()
        results["sd"] = sdJob.await()

        esClient.indices().refresh { r -> r.index(INDEX_NAME) }
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

    // ==================== SGG 인덱싱 (RDB emd_10 -> ES SGG) ====================

    private suspend fun reindexSggInternal(): Map<String, Any> = coroutineScope {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] ========== SGG 인덱싱 시작 (RDB 기반) ==========")

        val maxId = pnuAggCursorRepo.maxIdEmd10()
        log.info("[LDRC] SGG maxId: {}", maxId)

        val chunkSize = (maxId + PARALLELISM - 1) / PARALLELISM
        log.info("[LDRC] SGG {}개 워커로 병렬 처리 시작 (워커당 ~{} 건)", PARALLELISM, chunkSize)

        val processedCount = LongAdder()

        // 1. 각 워커가 자신의 aggMap 반환
        val jobs = (0 until PARALLELISM).map { workerIdx ->
            val workerMinId = workerIdx * chunkSize
            val workerMaxId = minOf((workerIdx + 1) * chunkSize, maxId)
            async(Dispatchers.IO) {
                processSggPartition(workerIdx, workerMinId, workerMaxId, maxId, processedCount)
            }
        }
        val partialMaps = jobs.awaitAll()

        // 2. 모든 워커 결과 merge
        log.info("[LDRC] SGG 워커 결과 merge 시작")
        val mergedMap = mutableMapOf<String, LdrcAggData>()
        for (partialMap in partialMaps) {
            for ((key, data) in partialMap) {
                mergedMap.compute(key) { _, existing ->
                    if (existing == null) {
                        data
                    } else {
                        existing.also { it.add(data.count.toInt(), data.sumLat, data.sumLng) }
                    }
                }
            }
        }
        log.info("[LDRC] SGG merge 완료: {} 건", mergedMap.size)

        // 3. merge된 결과 ES에 인덱싱
        val docs = mergedMap.values.map { it.toDocument("SGG") }
        docs.chunked(ES_BATCH_SIZE).forEach { chunk ->
            bulkIndex(chunk)
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LDRC] SGG 완료: {} 건, {}ms", docs.size, elapsed)
        mapOf("level" to "SGG", "indexed" to docs.size, "elapsedMs" to elapsed)
    }

    private fun processSggPartition(
        workerIdx: Int,
        minId: Long,
        maxId: Long,
        totalMaxId: Long,
        processedCount: LongAdder
    ): Map<String, LdrcAggData> {
        val aggMap = mutableMapOf<String, LdrcAggData>()

        var lastId = minId
        while (true) {
            val rows = pnuAggCursorRepo.findEmd10ByIdRange(lastId, maxId, BATCH_SIZE)
            if (rows.isEmpty()) break

            for (row in rows) {
                val codeStr = row.code.toString()
                val sggCode = codeStr.take(5)
                val sdCode = codeStr.take(2)
                val parentH3 = h3.cellToParent(row.h3Index, 7)
                val aggKey = "${sggCode}_${parentH3}"

                aggMap.compute(aggKey) { _, existing ->
                    (existing ?: LdrcAggData(parentH3, sggCode, sdCode, sggCode)).also {
                        it.add(row.cnt, row.sumLat, row.sumLng)
                    }
                }
            }

            lastId = rows.last().id
            processedCount.add(rows.size.toLong())

            val done = processedCount.sum()
            if (done % 100000 == 0L) {
                log.info("[LDRC] SGG Worker-{} id={} - {}/{}", workerIdx, lastId, done, totalMaxId)
            }
        }

        log.info("[LDRC] SGG Worker-{} 집계 완료: {} 건", workerIdx, aggMap.size)
        return aggMap
    }

    // ==================== SD 인덱싱 (RDB emd_10 -> ES SD) ====================

    private suspend fun reindexSdInternal(): Map<String, Any> = coroutineScope {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] ========== SD 인덱싱 시작 (RDB 기반) ==========")

        val maxId = pnuAggCursorRepo.maxIdEmd10()
        log.info("[LDRC] SD maxId: {}", maxId)

        val chunkSize = (maxId + PARALLELISM - 1) / PARALLELISM
        log.info("[LDRC] SD {}개 워커로 병렬 처리 시작 (워커당 ~{} 건)", PARALLELISM, chunkSize)

        val processedCount = LongAdder()

        // 1. 각 워커가 자신의 aggMap 반환
        val jobs = (0 until PARALLELISM).map { workerIdx ->
            val workerMinId = workerIdx * chunkSize
            val workerMaxId = minOf((workerIdx + 1) * chunkSize, maxId)
            async(Dispatchers.IO) {
                processSdPartition(workerIdx, workerMinId, workerMaxId, maxId, processedCount)
            }
        }
        val partialMaps = jobs.awaitAll()

        // 2. 모든 워커 결과 merge
        log.info("[LDRC] SD 워커 결과 merge 시작")
        val mergedMap = mutableMapOf<String, LdrcAggData>()
        for (partialMap in partialMaps) {
            for ((key, data) in partialMap) {
                mergedMap.compute(key) { _, existing ->
                    if (existing == null) {
                        data
                    } else {
                        existing.also { it.add(data.count.toInt(), data.sumLat, data.sumLng) }
                    }
                }
            }
        }
        log.info("[LDRC] SD merge 완료: {} 건", mergedMap.size)

        // 3. merge된 결과 ES에 인덱싱
        val docs = mergedMap.values.map { it.toDocument("SD") }
        docs.chunked(ES_BATCH_SIZE).forEach { chunk ->
            bulkIndex(chunk)
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LDRC] SD 완료: {} 건, {}ms", docs.size, elapsed)
        mapOf("level" to "SD", "indexed" to docs.size, "elapsedMs" to elapsed)
    }

    private fun processSdPartition(
        workerIdx: Int,
        minId: Long,
        maxId: Long,
        totalMaxId: Long,
        processedCount: LongAdder
    ): Map<String, LdrcAggData> {
        val aggMap = mutableMapOf<String, LdrcAggData>()

        var lastId = minId
        while (true) {
            val rows = pnuAggCursorRepo.findEmd10ByIdRange(lastId, maxId, BATCH_SIZE)
            if (rows.isEmpty()) break

            for (row in rows) {
                val codeStr = row.code.toString()
                val sdCode = codeStr.take(2)
                val parentH3 = h3.cellToParent(row.h3Index, 5)
                val aggKey = "${sdCode}_${parentH3}"

                aggMap.compute(aggKey) { _, existing ->
                    (existing ?: LdrcAggData(parentH3, sdCode, sdCode, "")).also {
                        it.add(row.cnt, row.sumLat, row.sumLng)
                    }
                }
            }

            lastId = rows.last().id
            processedCount.add(rows.size.toLong())

            val done = processedCount.sum()
            if (done % 100000 == 0L) {
                log.info("[LDRC] SD Worker-{} id={} - {}/{}", workerIdx, lastId, done, totalMaxId)
            }
        }

        log.info("[LDRC] SD Worker-{} 집계 완료: {} 건", workerIdx, aggMap.size)
        return aggMap
    }

    // ==================== 공통 ====================

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
