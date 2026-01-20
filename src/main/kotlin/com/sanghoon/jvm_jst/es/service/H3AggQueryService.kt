package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import com.sanghoon.jvm_jst.rds.common.BBox
import com.sanghoon.jvm_jst.rds.common.H3Util
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class H3AggQueryService(
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = "land_h3_agg"

        // 시도코드 → 이름 매핑
        val SIDO_NAMES = mapOf(
            11L to "서울특별시",
            26L to "부산광역시",
            27L to "대구광역시",
            28L to "인천광역시",
            29L to "광주광역시",
            30L to "대전광역시",
            31L to "울산광역시",
            36L to "세종특별자치시",
            41L to "경기도",
            43L to "충청북도",
            44L to "충청남도",
            46L to "전라남도",
            47L to "경상북도",
            48L to "경상남도",
            50L to "제주특별자치도",
            51L to "강원특별자치도",
            52L to "전북특별자치도"
        )

        // 레벨별 H3 resolution
        val LEVEL_RESOLUTION = mapOf(
            "SD" to 5,
            "SGG" to 7,
            "EMD" to 10
        )
    }

    /**
     * bbox로 H3 집계 조회 (무필터용)
     */
    fun queryByBBox(
        swLng: Double,
        swLat: Double,
        neLng: Double,
        neLat: Double,
        level: String
    ): H3AggResponse {
        val startTime = System.currentTimeMillis()
        val bbox = BBox(swLng, swLat, neLng, neLat)
        val resolution = LEVEL_RESOLUTION[level] ?: 5

        // bbox → H3 인덱스 목록
        val h3Start = System.currentTimeMillis()
        val h3Indexes = H3Util.bboxToH3Indexes(bbox, resolution)
        val h3Time = System.currentTimeMillis() - h3Start

        if (h3Indexes.isEmpty()) {
            log.info("[H3Agg query] H3 인덱스 없음 (한국 영역 밖)")
            return H3AggResponse(
                level = level,
                h3Count = 0,
                clusters = emptyList(),
                totalCount = 0,
                elapsedMs = System.currentTimeMillis() - startTime
            )
        }

        log.info("[H3Agg query] level={}, H3 인덱스: {}개, H3 변환: {}ms", level, h3Indexes.size, h3Time)

        // ES 쿼리
        val esStart = System.currentTimeMillis()
        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(0)
                .query(buildQuery(level, h3Indexes))
                .aggregations("by_region", Aggregation.of { agg ->
                    agg.terms { t -> t.field("regionCode").size(1000) }
                        .aggregations(mapOf(
                            "sum_cnt" to Aggregation.of { a -> a.sum { sum -> sum.field("cnt") } },
                            "sum_lat" to Aggregation.of { a -> a.sum { sum -> sum.field("sumLat") } },
                            "sum_lng" to Aggregation.of { a -> a.sum { sum -> sum.field("sumLng") } }
                        ))
                })
        }, Void::class.java)
        val esTime = System.currentTimeMillis() - esStart

        // 결과 파싱
        val regionAgg = response.aggregations()["by_region"]?.lterms()
        val clusters = regionAgg?.buckets()?.array()?.map { bucket ->
            val code = bucket.key()
            val totalCnt = bucket.aggregations()["sum_cnt"]?.sum()?.value()?.toLong() ?: 0
            val totalLat = bucket.aggregations()["sum_lat"]?.sum()?.value() ?: 0.0
            val totalLng = bucket.aggregations()["sum_lng"]?.sum()?.value() ?: 0.0

            val centerLat = if (totalCnt > 0) totalLat / totalCnt else 0.0
            val centerLng = if (totalCnt > 0) totalLng / totalCnt else 0.0

            H3AggCluster(
                code = code,
                name = getRegionName(level, code),
                count = totalCnt,
                centerLat = centerLat,
                centerLng = centerLng
            )
        } ?: emptyList()

        val totalCount = clusters.sumOf { it.count }
        val totalElapsed = System.currentTimeMillis() - startTime

        log.info("[H3Agg query] ES 쿼리: {}ms, 클러스터: {}개, 총 필지: {}, 전체: {}ms",
            esTime, clusters.size, totalCount, totalElapsed)

        return H3AggResponse(
            level = level,
            h3Count = h3Indexes.size,
            clusters = clusters,
            totalCount = totalCount,
            elapsedMs = totalElapsed
        )
    }

    private fun buildQuery(level: String, h3Indexes: List<Long>): Query {
        return Query.of { q ->
            q.bool { bool ->
                bool.filter { f ->
                    f.term { t -> t.field("level").value(level) }
                }
                bool.filter { f ->
                    f.terms { t ->
                        t.field("h3Index").terms { tv ->
                            tv.value(h3Indexes.map { FieldValue.of(it) })
                        }
                    }
                }
                bool
            }
        }
    }

    private fun getRegionName(level: String, code: Long): String {
        return when (level) {
            "SD" -> SIDO_NAMES[code] ?: "알 수 없음"
            else -> code.toString() // 시군구/읍면동은 코드로 표시 (필요시 별도 매핑)
        }
    }
}

data class H3AggResponse(
    val level: String,
    val h3Count: Int,
    val clusters: List<H3AggCluster>,
    val totalCount: Long,
    val elapsedMs: Long
)

data class H3AggCluster(
    val code: Long,
    val name: String,
    val count: Long,
    val centerLat: Double,
    val centerLng: Double
)
