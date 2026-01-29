package com.datahub.geo_poc.es.service.ldrc

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import com.datahub.geo_poc.es.document.cluster.LdrcDocument
import com.datahub.geo_poc.jpa.repository.PnuAggEmd10Repository
import com.uber.h3core.H3Core
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.NumberFormat
import java.util.Locale

@Service
class LdrcIndexingService(
    private val emd10Repository: PnuAggEmd10Repository,
    private val esClient: OpenSearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val h3 = H3Core.newInstance()
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    companion object {
        const val INDEX_NAME = LdrcDocument.INDEX_NAME
        const val EMD_BULK_SIZE = 10000
    }

    private fun formatElapsed(ms: Long): String = "${numberFormat.format(ms)}ms (${String.format("%.2f", ms / 1000.0)}s)"
    private fun formatCount(n: Number): String = numberFormat.format(n)

    /**
     * LDRC 전체 인덱싱 (스트리밍 방식)
     * 1. PostgreSQL emd_10 → ES EMD (스트리밍)
     * 2. ES EMD → 집계 → ES SGG
     * 3. ES SGG → 집계 → ES SD
     */
    @Transactional(readOnly = true)
    fun reindex(): Map<String, Any> {
        val totalStartTime = System.currentTimeMillis()
        log.info("[LDRC] ========== 전체 인덱싱 시작 ==========")

        // 인덱스 생성
        ensureIndexExists()

        val results = mutableMapOf<String, Any>()
        var totalDocs = 0

        // 1. EMD (읍면동, res 10) - PostgreSQL에서 스트리밍 조회
        val emdResult = indexEmdStreaming()
        results["emd"] = emdResult
        totalDocs += emdResult["count"] as Int

        // 2. SGG (시군구, res 7) - ES Aggregation
        val sggStart = System.currentTimeMillis()
        log.info("[LDRC] ========== SGG 집계 시작 ==========")
        val sggDocs = aggregateWithEsAgg("SGG")
        val sggAggTime = System.currentTimeMillis() - sggStart
        log.info("[LDRC] SGG 집계 완료: {}건, {}", formatCount(sggDocs.size), formatElapsed(sggAggTime))

        val sggIndexStart = System.currentTimeMillis()
        bulkIndex(sggDocs)
        val sggIndexTime = System.currentTimeMillis() - sggIndexStart
        val sggTotalTime = System.currentTimeMillis() - sggStart
        log.info("[LDRC] SGG 인덱싱 완료: {}건, 인덱싱 {}, 총 {}", formatCount(sggDocs.size), formatElapsed(sggIndexTime), formatElapsed(sggTotalTime))
        results["sgg"] = mapOf("count" to sggDocs.size, "aggregateMs" to sggAggTime, "indexMs" to sggIndexTime, "totalMs" to sggTotalTime)
        totalDocs += sggDocs.size

        // 3. SD (시도, res 5) - ES Aggregation
        val sdStart = System.currentTimeMillis()
        log.info("[LDRC] ========== SD 집계 시작 ==========")
        val sdDocs = aggregateWithEsAgg("SD")
        val sdAggTime = System.currentTimeMillis() - sdStart
        log.info("[LDRC] SD 집계 완료: {}건, {}", formatCount(sdDocs.size), formatElapsed(sdAggTime))

        val sdIndexStart = System.currentTimeMillis()
        bulkIndex(sdDocs)
        val sdIndexTime = System.currentTimeMillis() - sdIndexStart
        val sdTotalTime = System.currentTimeMillis() - sdStart
        log.info("[LDRC] SD 인덱싱 완료: {}건, 인덱싱 {}, 총 {}", formatCount(sdDocs.size), formatElapsed(sdIndexTime), formatElapsed(sdTotalTime))
        results["sd"] = mapOf("count" to sdDocs.size, "aggregateMs" to sdAggTime, "indexMs" to sdIndexTime, "totalMs" to sdTotalTime)
        totalDocs += sdDocs.size

        // forcemerge 비활성화: 집계 기반 워크로드에서 실효성 없음
        // (original forcemerge code commented out)
        // log.info("[LDRC] ========== Forcemerge 시작 (비동기) ==========")
        // esClient.indices().forcemerge { f ->
        //     f.index(INDEX_NAME)
        //         .maxNumSegments(1L)
        // }
        // log.info("[LDRC] Forcemerge 요청 완료 (백그라운드 실행 중)")

        val totalElapsed = System.currentTimeMillis() - totalStartTime
        log.info("[LDRC] ========== 전체 완료 ==========")
        log.info("[LDRC] 총 문서: {}건, 총 소요시간: {}", formatCount(totalDocs), formatElapsed(totalElapsed))

        results["total"] = mapOf("count" to totalDocs, "elapsedMs" to totalElapsed)
        results["forcemerge"] = "disabled"
        results["success"] = true

        return results
    }

    /**
     * EMD 스트리밍 인덱싱 (PostgreSQL → ES)
     */
    private fun indexEmdStreaming(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] ========== EMD 스트리밍 인덱싱 시작 ==========")

        // 전체 개수 먼저 조회
        val countStart = System.currentTimeMillis()
        val totalExpected = emd10Repository.count()
        val countTime = System.currentTimeMillis() - countStart
        val expectedBulks = (totalExpected + EMD_BULK_SIZE - 1) / EMD_BULK_SIZE
        log.info("[LDRC] EMD 전체 개수: {}건, 예상 벌크 {}회, 조회 {}",
            formatCount(totalExpected), formatCount(expectedBulks), formatElapsed(countTime))

        val buffer = mutableListOf<LdrcDocument>()
        var processedCount = 0
        var bulkCount = 0

        emd10Repository.findAllBy().use { stream ->
            stream.forEach { entity ->
                buffer.add(LdrcDocument.fromEmd10(entity, h3))
                processedCount++

                if (buffer.size >= EMD_BULK_SIZE) {
                    bulkCount++
                    val bulkStart = System.currentTimeMillis()
                    bulkIndex(buffer)
                    val bulkTime = System.currentTimeMillis() - bulkStart
                    val elapsed = System.currentTimeMillis() - startTime
                    val percent = String.format("%.1f", processedCount * 100.0 / totalExpected)
                    log.info("[LDRC] EMD 벌크 #{}/{}: {}/{} ({}%) 완료, 벌크 {}, 누적 {}",
                        formatCount(bulkCount), formatCount(expectedBulks),
                        formatCount(processedCount), formatCount(totalExpected), percent,
                        formatElapsed(bulkTime), formatElapsed(elapsed))
                    buffer.clear()
                }
            }

            // 남은 버퍼 처리
            if (buffer.isNotEmpty()) {
                bulkCount++
                val bulkStart = System.currentTimeMillis()
                bulkIndex(buffer)
                val bulkTime = System.currentTimeMillis() - bulkStart
                val elapsed = System.currentTimeMillis() - startTime
                log.info("[LDRC] EMD 벌크 #{}/{} (마지막): {}/{} (100.0%) 완료, 벌크 {}, 누적 {}",
                    formatCount(bulkCount), formatCount(expectedBulks),
                    formatCount(processedCount), formatCount(totalExpected),
                    formatElapsed(bulkTime), formatElapsed(elapsed))
                buffer.clear()
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        log.info("[LDRC] EMD 스트리밍 인덱싱 완료: 총 {}건, 벌크 {}회, 총 소요시간 {}",
            formatCount(processedCount), formatCount(bulkCount), formatElapsed(totalTime))

        return mapOf(
            "count" to processedCount,
            "bulkCount" to bulkCount,
            "totalMs" to totalTime
        )
    }

    /**
     * ES Aggregation으로 SGG/SD 집계
     * - SGG: sgg7 + regionCodeSgg 기준
     * - SD: sd5 + regionCodeSd 기준
     */
    private fun aggregateWithEsAgg(targetLevel: String): List<LdrcDocument> {
        val startTime = System.currentTimeMillis()
        val docs = mutableListOf<LdrcDocument>()

        val h3Field = if (targetLevel == "SGG") "sgg7" else "sd5"
        val regionField = if (targetLevel == "SGG") "regionCodeSgg" else "regionCodeSd"

        log.info("[LDRC] {} ES Aggregation 시작: {} + {} 기준", targetLevel, h3Field, regionField)

        // Composite aggregation으로 페이징하며 집계
        var afterKey: Map<String, String>? = null
        var totalBuckets = 0
        var batchCount = 0

        while (true) {
            val batchStart = System.currentTimeMillis()
            batchCount++

            val response = esClient.search({ s ->
                s.index(INDEX_NAME)
                    .size(0)
                    .query { q -> q.term { t -> t.field("level").value(org.opensearch.client.opensearch._types.FieldValue.of("EMD")) } }
                    .aggregations("composite_agg") { a ->
                        a.composite { c ->
                            c.size(10000)
                                .sources(listOf(
                                    mapOf("h3" to org.opensearch.client.opensearch._types.aggregations.CompositeAggregationSource.of { src ->
                                        src.terms { t -> t.field(h3Field) }
                                    }),
                                    mapOf("region" to org.opensearch.client.opensearch._types.aggregations.CompositeAggregationSource.of { src ->
                                        src.terms { t -> t.field(regionField) }
                                    })
                                ))
                                .apply {
                                    if (afterKey != null) {
                                        after(afterKey)
                                    }
                                }
                        }
                            .aggregations("sum_cnt") { sub -> sub.sum { sum -> sum.field("cnt") } }
                            .aggregations("sum_lat") { sub -> sub.sum { sum -> sum.field("sumLat") } }
                            .aggregations("sum_lng") { sub -> sub.sum { sum -> sum.field("sumLng") } }
                    }
            }, Void::class.java)

            val compositeAgg = response.aggregations()["composite_agg"]?.composite()
                ?: break

            val buckets = compositeAgg.buckets().array()
            if (buckets.isEmpty()) break

            for (bucket in buckets) {
                val h3Index = bucket.key()["h3"]?.toString()?.toLongOrNull() ?: continue
                val regionCode = bucket.key()["region"]?.toString()?.toLongOrNull() ?: continue
                val cnt = bucket.aggregations()["sum_cnt"]?.sum()?.value()?.toInt() ?: 0
                val sumLat = bucket.aggregations()["sum_lat"]?.sum()?.value() ?: 0.0
                val sumLng = bucket.aggregations()["sum_lng"]?.sum()?.value() ?: 0.0

                docs.add(LdrcDocument(
                    id = "${targetLevel}_${h3Index}_${regionCode}",
                    level = targetLevel,
                    h3Index = h3Index,
                    regionCode = regionCode,
                    cnt = cnt,
                    sumLat = sumLat,
                    sumLng = sumLng
                ))
            }

            totalBuckets += buckets.size
            val batchTime = System.currentTimeMillis() - batchStart
            val elapsed = System.currentTimeMillis() - startTime
            log.info("[LDRC] {} 배치 #{}: {}건 처리, 누적 {}건, 배치 {}, 누적 {}",
                targetLevel, formatCount(batchCount), formatCount(buckets.size),
                formatCount(totalBuckets), formatElapsed(batchTime), formatElapsed(elapsed))

            // 다음 페이지
            afterKey = compositeAgg.afterKey()?.mapValues { it.value.toString() }
            if (afterKey == null || buckets.size < 10000) break
        }

        val totalTime = System.currentTimeMillis() - startTime
        log.info("[LDRC] {} ES Aggregation 완료: {}건, 총 {}",
            targetLevel, formatCount(docs.size), formatElapsed(totalTime))

        return docs
    }

    private fun bulkIndex(docs: List<LdrcDocument>) {
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
            log.warn("[LDRC] bulkIndex 일부 실패: {}/{}", formatCount(failedCount), formatCount(docs.size))
        }
    }

    private fun bulkIndexChunked(docs: List<LdrcDocument>, chunkSize: Int) {
        val startTime = System.currentTimeMillis()
        val totalChunks = (docs.size + chunkSize - 1) / chunkSize

        docs.chunked(chunkSize).forEachIndexed { idx, chunk ->
            val chunkStart = System.currentTimeMillis()
            bulkIndex(chunk)
            val chunkTime = System.currentTimeMillis() - chunkStart
            val processed = minOf((idx + 1) * chunkSize, docs.size)
            val elapsed = System.currentTimeMillis() - startTime
            val percent = String.format("%.1f", processed * 100.0 / docs.size)
            log.info("[LDRC] 청크 #{}/{}: {}/{} ({}%) 완료, 청크 {}, 누적 {}",
                formatCount(idx + 1), formatCount(totalChunks),
                formatCount(processed), formatCount(docs.size), percent,
                formatElapsed(chunkTime), formatElapsed(elapsed))
        }
    }

    private fun ensureIndexExists() {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] 인덱스 확인 중: {}", INDEX_NAME)

        val exists = try {
            esClient.indices().exists { e -> e.index(INDEX_NAME) }.value()
        } catch (e: Exception) {
            false
        }

        if (exists) {
            val deleteStart = System.currentTimeMillis()
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            val deleteTime = System.currentTimeMillis() - deleteStart
            log.info("[LDRC] 기존 인덱스 삭제 완료: {}", formatElapsed(deleteTime))
        }

        val createStart = System.currentTimeMillis()
        esClient.indices().create { c ->
            c.index(INDEX_NAME)
                .settings { s ->
                    s.numberOfShards("1")
                        .numberOfReplicas("0")
                }
                .mappings { m ->
                    m.properties("id") { p -> p.keyword { it } }
                        .properties("level") { p -> p.keyword { it } }
                        .properties("h3Index") { p -> p.long_ { it } }
                        .properties("regionCode") { p -> p.long_ { it } }
                        .properties("cnt") { p -> p.integer { it } }
                        .properties("sumLat") { p -> p.double_ { it } }
                        .properties("sumLng") { p -> p.double_ { it } }
                        .properties("sgg7") { p -> p.long_ { it } }
                        .properties("sd5") { p -> p.long_ { it } }
                        .properties("regionCodeSgg") { p -> p.long_ { it } }
                        .properties("regionCodeSd") { p -> p.long_ { it } }
                }
        }
        val createTime = System.currentTimeMillis() - createStart
        val totalTime = System.currentTimeMillis() - startTime
        log.info("[LDRC] 인덱스 생성 완료: {}, 생성 {}, 총 {}", INDEX_NAME, formatElapsed(createTime), formatElapsed(totalTime))
    }

    fun count(): Long {
        return try {
            esClient.count { c -> c.index(INDEX_NAME) }.count()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * EMD 인덱싱 (인덱스 생성 + PostgreSQL → ES, 스트리밍 방식)
     */
    @Transactional(readOnly = true)
    fun reindexEmd(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] ========== EMD 단독 인덱싱 시작 ==========")

        // 인덱스 생성
        ensureIndexExists()

        // EMD 스트리밍 인덱싱
        val emdResult = indexEmdStreaming()

        val totalElapsed = System.currentTimeMillis() - startTime
        log.info("[LDRC] EMD 단독 인덱싱 완료: 총 {}건, 총 소요시간 {}",
            formatCount(emdResult["count"] as Int), formatElapsed(totalElapsed))

        return mapOf(
            "level" to "EMD",
            "count" to emdResult["count"]!!,
            "bulkCount" to emdResult["bulkCount"]!!,
            "totalMs" to totalElapsed,
            "success" to true
        )
    }

    /**
     * SGG 인덱싱 (ES EMD → ES Aggregation → ES SGG)
     */
    fun reindexSgg(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] ========== SGG 단독 인덱싱 시작 ==========")

        val sggDocs = aggregateWithEsAgg("SGG")
        val aggregateTime = System.currentTimeMillis() - startTime

        val indexStart = System.currentTimeMillis()
        bulkIndexChunked(sggDocs, 10000)
        val indexTime = System.currentTimeMillis() - indexStart

        val totalElapsed = System.currentTimeMillis() - startTime
        log.info("[LDRC] SGG 단독 인덱싱 완료: {}건, 집계 {}, 인덱싱 {}, 총 {}",
            formatCount(sggDocs.size), formatElapsed(aggregateTime), formatElapsed(indexTime), formatElapsed(totalElapsed))

        return mapOf(
            "level" to "SGG",
            "count" to sggDocs.size,
            "aggregateMs" to aggregateTime,
            "indexMs" to indexTime,
            "totalMs" to totalElapsed,
            "success" to true
        )
    }

    /**
     * SD 인덱싱 (ES EMD → ES Aggregation → ES SD)
     */
    fun reindexSd(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] ========== SD 단독 인덱싱 시작 ==========")

        val sdDocs = aggregateWithEsAgg("SD")
        val aggregateTime = System.currentTimeMillis() - startTime

        val indexStart = System.currentTimeMillis()
        bulkIndexChunked(sdDocs, 10000)
        val indexTime = System.currentTimeMillis() - indexStart

        val totalElapsed = System.currentTimeMillis() - startTime
        log.info("[LDRC] SD 단독 인덱싱 완료: {}건, 집계 {}, 인덱싱 {}, 총 {}",
            formatCount(sdDocs.size), formatElapsed(aggregateTime), formatElapsed(indexTime), formatElapsed(totalElapsed))

        return mapOf(
            "level" to "SD",
            "count" to sdDocs.size,
            "aggregateMs" to aggregateTime,
            "indexMs" to indexTime,
            "totalMs" to totalElapsed,
            "success" to true
        )
    }

    /**
     * Forcemerge 실행 (비동기)
     */
    fun forcemerge(): Map<String, Any> {
        // forcemerge 비활성화: 집계 기반 워크로드에서 실효성 없음
        return mapOf(
            "action" to "forcemerge",
            "status" to "disabled",
            "reason" to "집계 기반 워크로드에서 실효성 없음"
        )
    }
}
