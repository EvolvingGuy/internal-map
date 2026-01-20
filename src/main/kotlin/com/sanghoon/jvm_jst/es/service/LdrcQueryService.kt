package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.FieldValue
import com.sanghoon.jvm_jst.es.document.LandDynamicRegionClusterDocument
import com.uber.h3core.H3Core
import com.uber.h3core.util.LatLng
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * LDRC 가변형 행정구역 클러스터 조회 서비스
 */
@Service
class LdrcQueryService(
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val h3 = H3Core.newInstance()

    companion object {
        const val INDEX_NAME = LandDynamicRegionClusterDocument.INDEX_NAME
    }

    /**
     * bbox 내 가변형 클러스터 조회 (행정구역별 그룹핑)
     * @param level SD, SGG, EMD
     */
    fun findByBbox(level: String, swLng: Double, swLat: Double, neLng: Double, neLat: Double): LdrcQueryResult {
        val startTime = System.currentTimeMillis()

        val h3Res = when (level) {
            "SD" -> 5
            "SGG" -> 7
            "EMD" -> 10
            else -> 10
        }

        // bbox 내 H3 셀 구하기
        val h3Cells = getH3CellsInBbox(swLng, swLat, neLng, neLat, h3Res)
        if (h3Cells.isEmpty()) {
            return LdrcQueryResult(emptyList(), 0, System.currentTimeMillis() - startTime)
        }

        // ES에서 해당 H3 셀들의 문서 조회
        val docs = queryByH3Cells(level, h3Cells)

        // 행정구역 코드별 그룹핑
        val grouped = docs.groupBy { it.code }
        val regions = grouped.map { (code, docList) ->
            val totalCount = docList.sumOf { it.count }
            val totalSumLat = docList.sumOf { it.sumLat }
            val totalSumLng = docList.sumOf { it.sumLng }
            val avgLat = if (totalCount > 0) totalSumLat / totalCount else 0.0
            val avgLng = if (totalCount > 0) totalSumLng / totalCount else 0.0

            LdrcRegionData(
                code = code,
                name = code.toString(),
                cnt = totalCount,
                centerLat = avgLat,
                centerLng = avgLng
            )
        }

        val totalCount = regions.sumOf { it.cnt }
        val elapsed = System.currentTimeMillis() - startTime

        log.info("[LDRC] level={}, h3Cells={}, docs={}, regions={}, totalCount={}, total={}ms",
            level, h3Cells.size, docs.size, regions.size, totalCount, elapsed)

        return LdrcQueryResult(regions, totalCount, elapsed)
    }

    private fun getH3CellsInBbox(swLng: Double, swLat: Double, neLng: Double, neLat: Double, res: Int): List<Long> {
        return try {
            val polygon = listOf(
                LatLng(swLat, swLng),
                LatLng(swLat, neLng),
                LatLng(neLat, neLng),
                LatLng(neLat, swLng),
                LatLng(swLat, swLng)
            )
            h3.polygonToCells(polygon, emptyList(), res).toList()
        } catch (e: Exception) {
            log.warn("[LdrcQuery] H3 polyfill 실패: {}", e.message)
            emptyList()
        }
    }

    private fun queryByH3Cells(level: String, h3Cells: List<Long>): List<LdrcDoc> {
        val results = mutableListOf<LdrcDoc>()
        val cellChunks = h3Cells.chunked(1000)

        for ((idx, chunk) in cellChunks.withIndex()) {
            val response = esClient.search({ s ->
                s.index(INDEX_NAME)
                    .size(10000)
                    .profile(true)
                    .query { q ->
                        q.bool { b ->
                            b.must { m -> m.term { t -> t.field("level").value(level) } }
                                .must { m -> m.terms { t -> t.field("h3Index").terms { tv -> tv.value(chunk.map { FieldValue.of(it) }) } } }
                        }
                    }
                    .source { src -> src.filter { f -> f.includes("code", "count", "sumLat", "sumLng") } }
            }, Map::class.java)

            val esTook = response.took()
            val hits = response.hits().hits()

            // profile 로깅 (첫 번째 청크만)
            if (idx == 0) {
                response.profile()?.shards()?.forEachIndexed { shardIdx, shardProfile ->
                    shardProfile.searches().forEach { search ->
                        search.query().forEach { queryProfile ->
                            val timeMs = String.format("%.2f", queryProfile.timeInNanos() / 1_000_000.0)
                            log.info("[LDRC Profile] 샤드[{}] {} - {}ms", shardIdx, queryProfile.type(), timeMs)
                        }
                    }
                }
            }

            log.info("[LDRC] chunk {}/{}, h3={}, hits={}, ES took={}ms", idx + 1, cellChunks.size, chunk.size, hits.size, esTook)

            hits.forEach { hit ->
                @Suppress("UNCHECKED_CAST")
                val source = hit.source() as? Map<String, Any> ?: return@forEach
                results.add(LdrcDoc(
                    code = (source["code"] as? Number)?.toLong() ?: 0,
                    count = (source["count"] as? Number)?.toInt() ?: 0,
                    sumLat = (source["sumLat"] as? Number)?.toDouble() ?: 0.0,
                    sumLng = (source["sumLng"] as? Number)?.toDouble() ?: 0.0
                ))
            }
        }

        return results
    }

    private data class LdrcDoc(val code: Long, val count: Int, val sumLat: Double, val sumLng: Double)
}

data class LdrcRegionData(
    val code: Long,
    val name: String,
    val cnt: Int,
    val centerLat: Double,
    val centerLng: Double
)

data class LdrcQueryResult(
    val regions: List<LdrcRegionData>,
    val totalCount: Int,
    val elapsedMs: Long
)
