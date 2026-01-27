package com.datahub.geo_poc.es.service.lsrc

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import com.datahub.geo_poc.es.document.cluster.LsrcDocument
import com.datahub.geo_poc.jpa.repository.LandCharacteristicRepository
import com.datahub.geo_poc.jpa.repository.BoundaryRegionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * LSRC (Land Static Region Cluster) 인덱싱 서비스
 * 고정형 행정구역 클러스터 - 비즈니스 필터 없는 경우 사용
 *
 * v3: DB 한 방 GROUP BY 집계
 */
@Service
class LsrcIndexingService(
    private val landCharRepo: LandCharacteristicRepository,
    private val boundaryRegionRepo: BoundaryRegionRepository,
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LsrcDocument.INDEX_NAME
        const val BULK_SIZE = 2000
    }

    /**
     * 전체 재인덱싱 (v3 - DB 한 방 GROUP BY)
     */
    fun reindex(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, Any>()

        // [1] 인덱스 생성
        ensureIndexExists()
        log.info("[LSRC] 인덱스 생성 완료")

        // [2] DB 한 방 집계
        log.info("[LSRC] DB 집계 시작...")
        val aggResults = landCharRepo.aggregateAllByEmd()
        val dbElapsedMs = System.currentTimeMillis() - startTime
        log.info("[LSRC] DB 집계 완료: {} 개 EMD, {}ms ({:.1f}s)",
            aggResults.size, dbElapsedMs, dbElapsedMs / 1000.0)

        if (aggResults.isEmpty()) {
            log.warn("[LSRC] 집계 결과가 없습니다. 인덱싱 중단.")
            return mapOf("success" to false, "reason" to "no aggregation results")
        }

        // [3] 앱에서 롤업 (EMD → SGG → SD)
        val sdAgg = mutableMapOf<String, LsrcDocument.AggData>()
        val sggAgg = mutableMapOf<String, LsrcDocument.AggData>()
        val emdAgg = mutableMapOf<String, LsrcDocument.AggData>()

        for (row in aggResults) {
            val code = row.code
            val cnt = row.cnt
            val sumLat = row.sumLat
            val sumLng = row.sumLng

            // EMD (8자리 → 10자리)
            val emdCode = code.padEnd(10, '0')
            emdAgg.computeIfAbsent(emdCode) { LsrcDocument.AggData(it) }
                .add(cnt, sumLat, sumLng)

            // SGG (5자리 → 10자리)
            val sggCode = code.take(5).padEnd(10, '0')
            sggAgg.computeIfAbsent(sggCode) { LsrcDocument.AggData(it) }
                .add(cnt, sumLat, sumLng)

            // SD (2자리 → 10자리)
            val sdCode = code.take(2).padEnd(10, '0')
            sdAgg.computeIfAbsent(sdCode) { LsrcDocument.AggData(it) }
                .add(cnt, sumLat, sumLng)
        }

        val rollupElapsedMs = System.currentTimeMillis() - startTime
        log.info("[LSRC] 롤업 완료: SD={}, SGG={}, EMD={}, elapsed: {}ms",
            sdAgg.size, sggAgg.size, emdAgg.size, rollupElapsedMs)

        // [4] 행정구역 이름 로드
        val regionNames = boundaryRegionRepo.findAll()
            .associate { it.regionCode to (it.regionKoreanName ?: "") }
        log.info("[LSRC] 행정구역 이름 로드: {} 개", regionNames.size)

        // [5] Document 변환
        val docs = mutableListOf<LsrcDocument>()

        for ((code, agg) in sdAgg) {
            val name = regionNames[code] ?: regionNames[code.take(2)] ?: ""
            docs.add(agg.toDocument("SD", name))
        }

        for ((code, agg) in sggAgg) {
            val name = regionNames[code] ?: regionNames[code.take(5)] ?: ""
            docs.add(agg.toDocument("SGG", name))
        }

        for ((code, agg) in emdAgg) {
            val name = regionNames[code] ?: ""
            docs.add(agg.toDocument("EMD", name))
        }

        log.info("[LSRC] 문서 생성: SD={}, SGG={}, EMD={}, total={}",
            sdAgg.size, sggAgg.size, emdAgg.size, docs.size)

        // [6] ES Bulk 인덱싱
        bulkIndexChunked(docs, BULK_SIZE)
        val indexElapsedMs = System.currentTimeMillis() - startTime
        log.info("[LSRC] ES 인덱싱 완료: {} 개, elapsed: {}ms ({:.1f}s)",
            docs.size, indexElapsedMs, indexElapsedMs / 1000.0)

        // [7] Forcemerge (백그라운드)
        log.info("[LSRC] forcemerge 시작 (백그라운드)...")
        try {
            esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
            log.info("[LSRC] forcemerge 완료")
        } catch (e: Exception) {
            log.info("[LSRC] forcemerge 요청 완료 (ES 백그라운드 처리 중): {}", e.message)
        }

        // [8] 결과 반환
        val totalElapsedMs = System.currentTimeMillis() - startTime
        val totalElapsedSec = totalElapsedMs / 1000.0
        log.info("[LSRC] 전체 완료: docs={}, elapsed: {}ms ({:.1f}s)",
            docs.size, totalElapsedMs, totalElapsedSec)

        results["sd"] = sdAgg.size
        results["sgg"] = sggAgg.size
        results["emd"] = emdAgg.size
        results["total"] = docs.size
        results["dbElapsedMs"] = dbElapsedMs
        results["elapsedMs"] = totalElapsedMs
        results["elapsedSec"] = totalElapsedSec
        results["success"] = true

        return results
    }

    fun count(): Long {
        return try {
            esClient.count { c -> c.index(INDEX_NAME) }.count()
        } catch (e: Exception) {
            0
        }
    }

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

    fun forcemerge(): Map<String, Any> {
        log.info("[LSRC] forcemerge 시작 (백그라운드)...")
        try {
            esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
            log.info("[LSRC] forcemerge 완료")
        } catch (e: Exception) {
            log.info("[LSRC] forcemerge 요청 완료 (ES 백그라운드 처리 중): {}", e.message)
        }

        return mapOf(
            "action" to "forcemerge",
            "success" to true
        )
    }

    // ==================== Private ====================

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

    private fun bulkIndex(docs: List<LsrcDocument>) {
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

    private fun bulkIndexChunked(docs: List<LsrcDocument>, chunkSize: Int) {
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
