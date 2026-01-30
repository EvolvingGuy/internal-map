package com.datahub.geo_poc.es.service.lsrc

import org.opensearch.client.opensearch.OpenSearchClient
import com.datahub.geo_poc.es.document.cluster.LsrcDocument
import com.datahub.geo_poc.es.model.LsrcQueryResponse
import com.datahub.geo_poc.es.model.LsrcRegionData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * LSRC 고정형 행정구역 클러스터 조회 서비스
 */
@Service
class LsrcQueryService(
    private val esClient: OpenSearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LsrcDocument.INDEX_NAME
    }

    /**
     * bbox 내 고정형 클러스터 조회
     * @param level SD, SGG, EMD
     */
    fun findByBbox(level: String, swLng: Double, swLat: Double, neLng: Double, neLat: Double): LsrcQueryResponse {
        val startTime = System.currentTimeMillis()

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(1000)
                .profile(false)
                .query { q ->
                    q.bool { b ->
                        b.must { m -> m.term { t -> t.field("level").value(org.opensearch.client.opensearch._types.FieldValue.of(level)) } }
                            .must { m ->
                                m.geoBoundingBox { geo ->
                                    geo.field("center")
                                        .boundingBox { bb ->
                                            bb.tlbr { tlbr ->
                                                tlbr.topLeft { tl -> tl.latlon { ll -> ll.lat(neLat).lon(swLng) } }
                                                    .bottomRight { br -> br.latlon { ll -> ll.lat(swLat).lon(neLng) } }
                                            }
                                        }
                                }
                            }
                    }
                }
                .source { src -> src.filter { f -> f.includes("code", "name", "count", "center") } }
        }, Map::class.java)

        val esTook = response.took()
        logProfile(response, esTook)

        val regions = response.hits().hits().mapNotNull { hit ->
            @Suppress("UNCHECKED_CAST")
            val source = hit.source() as? Map<String, Any> ?: return@mapNotNull null
            val center = source["center"] as? Map<*, *>
            LsrcRegionData(
                code = source["code"]?.toString() ?: "",
                name = source["name"]?.toString() ?: "",
                cnt = (source["count"] as? Number)?.toInt() ?: 0,
                centerLat = (center?.get("lat") as? Number)?.toDouble() ?: 0.0,
                centerLng = (center?.get("lon") as? Number)?.toDouble() ?: 0.0
            )
        }

        val totalCount = regions.sumOf { it.cnt }
        val elapsed = System.currentTimeMillis() - startTime

        log.info("[LSRC] level={}, regions={}, totalCount={}, ES took={}ms, total={}ms",
            level, regions.size, totalCount, esTook, elapsed)

        return LsrcQueryResponse(regions, totalCount, elapsed)
    }

    private fun logProfile(response: org.opensearch.client.opensearch.core.SearchResponse<Map<*, *>>, esTook: Long) {
        val profile = response.profile() ?: return
        val shards = profile.shards()

        shards.forEachIndexed { idx, shardProfile ->
            shardProfile.searches().forEach { search ->
                search.query().forEach { queryProfile ->
                    val timeMs = String.format("%.2f", queryProfile.timeInNanos() / 1_000_000.0)
                    log.info("[LSRC Profile] 샤드[{}] {} - {}ms", idx, queryProfile.type(), timeMs)
                }
            }
        }
    }
}
