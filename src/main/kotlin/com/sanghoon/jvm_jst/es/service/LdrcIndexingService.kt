package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import com.sanghoon.jvm_jst.es.document.LdrcDocument
import com.sanghoon.jvm_jst.rds.repository.PnuAggEmd10Repository
import com.uber.h3core.H3Core
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LdrcIndexingService(
    private val emd10Repository: PnuAggEmd10Repository,
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val h3 = H3Core.newInstance()

    companion object {
        const val INDEX_NAME = LdrcDocument.INDEX_NAME
    }

    /**
     * LDRC 전체 인덱싱
     * 1. PostgreSQL emd_10 → ES EMD
     * 2. ES EMD → 집계 → ES SGG
     * 3. ES SGG → 집계 → ES SD
     */
    fun reindex(): Map<String, Any> {
        val totalStartTime = System.currentTimeMillis()

        // 인덱스 생성
        ensureIndexExists()

        val results = mutableMapOf<String, Any>()
        var totalDocs = 0

        // 1. EMD (읍면동, res 10) - PostgreSQL에서 조회
        val emdStart = System.currentTimeMillis()
        val emdEntities = emd10Repository.findAll()
        val emdDbTime = System.currentTimeMillis() - emdStart
        log.info("[LDRC] EMD - PostgreSQL 조회: {}건, {}ms", emdEntities.size, emdDbTime)

        val emdIndexStart = System.currentTimeMillis()
        val emdDocs = emdEntities.map { LdrcDocument.fromEmd10(it) }
        bulkIndexChunked(emdDocs, 10000)
        val emdIndexTime = System.currentTimeMillis() - emdIndexStart
        log.info("[LDRC] EMD - ES 인덱싱: {}건, {}ms", emdDocs.size, emdIndexTime)
        results["emd"] = mapOf("count" to emdDocs.size, "dbMs" to emdDbTime, "indexMs" to emdIndexTime)
        totalDocs += emdDocs.size

        // 2. SGG (시군구, res 7) - ES EMD에서 집계
        val sggStart = System.currentTimeMillis()
        val sggDocs = aggregateFromEs("EMD", 7, "SGG")
        val sggTime = System.currentTimeMillis() - sggStart
        log.info("[LDRC] SGG - ES에서 집계: {}건, {}ms", sggDocs.size, sggTime)
        bulkIndex(sggDocs)
        results["sgg"] = mapOf("count" to sggDocs.size, "aggregateMs" to sggTime)
        totalDocs += sggDocs.size

        // 3. SD (시도, res 5) - ES SGG에서 집계
        val sdStart = System.currentTimeMillis()
        val sdDocs = aggregateFromEs("SGG", 5, "SD")
        val sdTime = System.currentTimeMillis() - sdStart
        log.info("[LDRC] SD - ES에서 집계: {}건, {}ms", sdDocs.size, sdTime)
        bulkIndex(sdDocs)
        results["sd"] = mapOf("count" to sdDocs.size, "aggregateMs" to sdTime)
        totalDocs += sdDocs.size

        // forcemerge
        val mergeStart = System.currentTimeMillis()
        log.info("[LDRC] forcemerge 시작...")
        esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
        val mergeTime = System.currentTimeMillis() - mergeStart
        log.info("[LDRC] forcemerge 완료: {}ms", mergeTime)

        val totalElapsed = System.currentTimeMillis() - totalStartTime
        log.info("[LDRC] 전체 완료 - 총 {}건, {}ms", totalDocs, totalElapsed)

        results["total"] = mapOf("count" to totalDocs, "elapsedMs" to totalElapsed, "mergeMs" to mergeTime)
        results["success"] = true

        return results
    }

    /**
     * ES에서 하위 레벨 데이터 읽어서 상위 레벨로 집계
     */
    private fun aggregateFromEs(sourceLevel: String, targetRes: Int, targetLevel: String): List<LdrcDocument> {
        val docs = mutableListOf<LdrcDocument>()

        // ES에서 sourceLevel 데이터 전체 조회 (search_after 방식)
        var lastSortValues: List<FieldValue>? = null
        val batchSize = 10000

        // (h3Index 상위 변환 + regionCode) 기준으로 집계
        // key: "${parentH3}_${regionCode}" → (cnt합, sumLat합, sumLng합)
        val aggMap = mutableMapOf<String, AggData>()

        while (true) {
            val response = esClient.search({ s ->
                s.index(INDEX_NAME)
                    .size(batchSize)
                    .query { q ->
                        q.term { t -> t.field("level").value(sourceLevel) }
                    }
                    .sort { sort -> sort.field { f -> f.field("id").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc) } }
                    .apply {
                        if (lastSortValues != null) {
                            searchAfter(lastSortValues)
                        }
                    }
            }, LdrcDocument::class.java)

            val hits = response.hits().hits()
            if (hits.isEmpty()) break

            for (hit in hits) {
                val doc = hit.source() ?: continue
                val parentH3 = h3.cellToParent(doc.h3Index, targetRes)
                // regionCode를 target level에 맞게 변환 (SGG: 5자리, SD: 2자리)
                val targetRegionCode = when (targetLevel) {
                    "SGG" -> doc.regionCode.toString().take(5).toLong()
                    "SD" -> doc.regionCode.toString().take(2).toLong()
                    else -> doc.regionCode
                }
                val key = "${parentH3}_${targetRegionCode}"

                aggMap.compute(key) { _, existing ->
                    if (existing == null) {
                        AggData(parentH3, targetRegionCode, doc.cnt.toLong(), doc.sumLat, doc.sumLng)
                    } else {
                        existing.copy(
                            cnt = existing.cnt + doc.cnt,
                            sumLat = existing.sumLat + doc.sumLat,
                            sumLng = existing.sumLng + doc.sumLng
                        )
                    }
                }
            }

            lastSortValues = hits.last().sort()
            log.info("[LDRC] {} → {} 집계 중: {}건 처리, 현재 집계 키: {}개",
                sourceLevel, targetLevel, hits.size, aggMap.size)
        }

        // 집계 결과를 문서로 변환
        for ((_, agg) in aggMap) {
            docs.add(LdrcDocument(
                id = "${targetLevel}_${agg.h3Index}_${agg.regionCode}",
                level = targetLevel,
                h3Index = agg.h3Index,
                regionCode = agg.regionCode,
                cnt = agg.cnt.toInt(),
                sumLat = agg.sumLat,
                sumLng = agg.sumLng
            ))
        }

        return docs
    }

    private data class AggData(
        val h3Index: Long,
        val regionCode: Long,
        val cnt: Long,
        val sumLat: Double,
        val sumLng: Double
    )

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
            log.warn("[LDRC bulkIndex] 일부 실패: {}/{}", failedCount, docs.size)
        }
    }

    private fun bulkIndexChunked(docs: List<LdrcDocument>, chunkSize: Int) {
        docs.chunked(chunkSize).forEachIndexed { idx, chunk ->
            bulkIndex(chunk)
            if ((idx + 1) % 10 == 0) {
                log.info("[LDRC] EMD 청크 진행: {}/{}", (idx + 1) * chunkSize, docs.size)
            }
        }
    }

    private fun ensureIndexExists() {
        val exists = try {
            esClient.indices().exists { e -> e.index(INDEX_NAME) }.value()
        } catch (e: Exception) {
            false
        }

        if (exists) {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            log.info("[LDRC] 기존 인덱스 삭제")
        }

        log.info("[LDRC] 인덱스 생성 - {}", INDEX_NAME)
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
                }
        }
    }

    fun count(): Long {
        return try {
            esClient.count { c -> c.index(INDEX_NAME) }.count()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * EMD 인덱싱 (인덱스 생성 + PostgreSQL → ES)
     */
    fun reindexEmd(): Map<String, Any> {
        val startTime = System.currentTimeMillis()

        // 인덱스 생성
        ensureIndexExists()

        // EMD (읍면동, res 10) - PostgreSQL에서 조회
        val dbStart = System.currentTimeMillis()
        val emdEntities = emd10Repository.findAll()
        val dbTime = System.currentTimeMillis() - dbStart
        log.info("[LDRC] EMD - PostgreSQL 조회: {}건, {}ms", emdEntities.size, dbTime)

        val indexStart = System.currentTimeMillis()
        val emdDocs = emdEntities.map { LdrcDocument.fromEmd10(it) }
        bulkIndexChunked(emdDocs, 10000)
        val indexTime = System.currentTimeMillis() - indexStart
        log.info("[LDRC] EMD - ES 인덱싱: {}건, {}ms", emdDocs.size, indexTime)

        val totalElapsed = System.currentTimeMillis() - startTime

        return mapOf(
            "level" to "EMD",
            "count" to emdDocs.size,
            "dbMs" to dbTime,
            "indexMs" to indexTime,
            "totalMs" to totalElapsed,
            "success" to true
        )
    }

    /**
     * SGG 인덱싱 (ES EMD → 집계 → ES SGG)
     */
    fun reindexSgg(): Map<String, Any> {
        val startTime = System.currentTimeMillis()

        val sggDocs = aggregateFromEs("EMD", 7, "SGG")
        val aggregateTime = System.currentTimeMillis() - startTime
        log.info("[LDRC] SGG - ES에서 집계: {}건, {}ms", sggDocs.size, aggregateTime)

        val indexStart = System.currentTimeMillis()
        bulkIndexChunked(sggDocs, 10000)
        val indexTime = System.currentTimeMillis() - indexStart
        log.info("[LDRC] SGG - ES 인덱싱: {}건, {}ms", sggDocs.size, indexTime)

        val totalElapsed = System.currentTimeMillis() - startTime

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
     * SD 인덱싱 (ES SGG → 집계 → ES SD)
     */
    fun reindexSd(): Map<String, Any> {
        val startTime = System.currentTimeMillis()

        val sdDocs = aggregateFromEs("SGG", 5, "SD")
        val aggregateTime = System.currentTimeMillis() - startTime
        log.info("[LDRC] SD - ES에서 집계: {}건, {}ms", sdDocs.size, aggregateTime)

        val indexStart = System.currentTimeMillis()
        bulkIndexChunked(sdDocs, 10000)
        val indexTime = System.currentTimeMillis() - indexStart
        log.info("[LDRC] SD - ES 인덱싱: {}건, {}ms", sdDocs.size, indexTime)

        val totalElapsed = System.currentTimeMillis() - startTime

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
     * Forcemerge 실행
     */
    fun forcemerge(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        log.info("[LDRC] forcemerge 시작...")
        esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LDRC] forcemerge 완료: {}ms", elapsed)

        return mapOf(
            "action" to "forcemerge",
            "elapsedMs" to elapsed,
            "success" to true
        )
    }
}
