package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import com.sanghoon.jvm_jst.es.document.LandClusterDocument
import com.sanghoon.jvm_jst.es.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LandClusterQueryService(
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = "land_cluster"

        // 시도코드 → 이름 매핑
        val SIDO_NAMES = mapOf(
            "11" to "서울특별시",
            "26" to "부산광역시",
            "27" to "대구광역시",
            "28" to "인천광역시",
            "29" to "광주광역시",
            "30" to "대전광역시",
            "31" to "울산광역시",
            "36" to "세종특별자치시",
            "41" to "경기도",
            "43" to "충청북도",
            "44" to "충청남도",
            "46" to "전라남도",
            "47" to "경상북도",
            "48" to "경상남도",
            "50" to "제주특별자치도",
            "51" to "강원특별자치도",
            "52" to "전북특별자치도"
        )
    }

    /**
     * 클러스터 조회 (sido 기준 aggregation)
     */
    fun getClusters(request: LandClusterRequest): LandClusterResponse {
        val startTime = System.currentTimeMillis()
        val query = buildQuery(request)

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(0)
                .profile(true) // 프로파일링 활성화
                .query(query)
                .aggregations("by_sido", Aggregation.of { agg ->
                    agg.terms { t -> t.field("sido").size(1000) }
                        .aggregations(mapOf(
                            "centroid" to Aggregation.of { a -> a.geoCentroid { gc -> gc.field("location") } }
                        ))
                })
        }, Void::class.java)

        // 프로파일 로깅
        val esTook = response.took()
        logProfile(response, esTook)

        val sidoAgg = response.aggregations()["by_sido"]?.sterms()
        val clusters = sidoAgg?.buckets()?.array()?.map { bucket ->
            val codeStr = bucket.key().stringValue()
            val code = codeStr.toIntOrNull() ?: 0
            val count = bucket.docCount()
            val centroid = bucket.aggregations()["centroid"]?.geoCentroid()?.location()
            val avgLat = centroid?.latlon()?.lat() ?: 0.0
            val avgLng = centroid?.latlon()?.lon() ?: 0.0

            ClusterItem(
                code = code,
                name = SIDO_NAMES[codeStr] ?: "알 수 없음",
                centerLat = avgLat,
                centerLng = avgLng,
                count = count
            )
        } ?: emptyList()

        val totalCount = clusters.sumOf { it.count }
        val totalElapsed = System.currentTimeMillis() - startTime

        log.info("[getClusters] 총 소요: {}ms (ES took: {}ms), 클러스터: {}개, 문서: {}건",
            totalElapsed, esTook, clusters.size, totalCount)

        return LandClusterResponse(
            clusterType = request.clusterType,
            totalCount = totalCount,
            clusters = clusters,
            elapsedMs = totalElapsed
        )
    }

    /**
     * 프로파일 결과 로깅
     */
    private fun logProfile(response: co.elastic.clients.elasticsearch.core.SearchResponse<Void>, esTook: Long) {
        val profile = response.profile() ?: return
        val shards = profile.shards()

        log.info("[Profile] ES took: {}ms, 샤드 수: {}", esTook, shards.size)

        shards.forEachIndexed { idx, shardProfile ->
            val searches = shardProfile.searches()

            searches.forEach { search ->
                search.query().forEach { queryProfile ->
                    val timeMs = String.format("%.2f", queryProfile.timeInNanos() / 1_000_000.0)
                    val queryType = queryProfile.type()
                    val breakdown = queryProfile.breakdown()

                    val nextDoc = breakdown.nextDoc()
                    val nextDocCount = breakdown.nextDocCount()
                    val buildScorer = breakdown.buildScorer()
                    val createWeight = breakdown.createWeight()

                    val nextDocMs = String.format("%.2f", nextDoc / 1_000_000.0)
                    val buildScorerMs = String.format("%.2f", buildScorer / 1_000_000.0)
                    val createWeightMs = String.format("%.2f", createWeight / 1_000_000.0)

                    log.info("[Profile] 샤드[{}] {} - 총: {}ms", idx, queryType, timeMs)
                    log.info("[Profile]   ├─ next_doc: {}ms ({}회)", nextDocMs, nextDocCount)
                    log.info("[Profile]   ├─ build_scorer: {}ms", buildScorerMs)
                    log.info("[Profile]   └─ create_weight: {}ms", createWeightMs)
                }
            }

            // Aggregation 프로파일
            shardProfile.aggregations().forEach { aggProfile ->
                val aggTimeMs = String.format("%.2f", aggProfile.timeInNanos() / 1_000_000.0)
                log.info("[Profile] 샤드[{}] Agg '{}': {}ms", idx, aggProfile.type(), aggTimeMs)
            }
        }
    }

    /**
     * PNU로 단건 조회
     */
    fun findByPnu(pnu: String): LandClusterDocument? {
        val response = esClient.get({ g ->
            g.index(INDEX_NAME).id(pnu)
        }, LandClusterDocument::class.java)

        return if (response.found()) response.source() else null
    }

    /**
     * 인덱스 카운트
     */
    fun count(): Long {
        val response = esClient.count { c -> c.index(INDEX_NAME) }
        return response.count()
    }

    /**
     * bbox + 필터 조건으로 Bool 쿼리 빌드
     */
    private fun buildQuery(request: LandClusterRequest): Query {
        return Query.of { q ->
            q.bool { bool ->
                // bbox 필터 - geo_bounding_box
                bool.filter { f ->
                    f.geoBoundingBox { geo ->
                        geo.field("location")
                            .boundingBox { bb ->
                                bb.tlbr { t ->
                                    t.topLeft { tl -> tl.latlon { ll -> ll.lat(request.neLat).lon(request.swLng) } }
                                     .bottomRight { br -> br.latlon { ll -> ll.lat(request.swLat).lon(request.neLng) } }
                                }
                            }
                    }
                }

                // jimokCd 필터
                if (request.jimokCd.isNotEmpty()) {
                    bool.filter { f ->
                        f.terms { t ->
                            t.field("jimokCd").terms { tv ->
                                tv.value(request.jimokCd.map { FieldValue.of(it) })
                            }
                        }
                    }
                }

                // jiyukCd1 필터
                if (request.jiyukCd1.isNotEmpty()) {
                    bool.filter { f ->
                        f.terms { t ->
                            t.field("jiyukCd1").terms { tv ->
                                tv.value(request.jiyukCd1.map { FieldValue.of(it) })
                            }
                        }
                    }
                }

                // mainPurpsCd 필터
                if (request.mainPurpsCd.isNotEmpty()) {
                    bool.filter { f ->
                        f.terms { t ->
                            t.field("mainPurpsCd").terms { tv ->
                                tv.value(request.mainPurpsCd.map { FieldValue.of(it) })
                            }
                        }
                    }
                }

                bool
            }
        }
    }
}
