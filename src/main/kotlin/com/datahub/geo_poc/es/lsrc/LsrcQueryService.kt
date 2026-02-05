package com.datahub.geo_poc.es.lsrc

import org.opensearch.client.opensearch.OpenSearchClient
import com.datahub.geo_poc.model.LsrcQueryResponse
import com.datahub.geo_poc.model.LsrcRegionData
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

        log.info("[LSRC] level={}, regions={}, totalCount={}, total={}ms",
            level, regions.size, totalCount, elapsed)

        return LsrcQueryResponse(regions, totalCount, elapsed)
    }
}
