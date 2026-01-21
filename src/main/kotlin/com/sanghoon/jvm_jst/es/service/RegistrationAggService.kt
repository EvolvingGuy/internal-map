package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.FieldValue
import com.sanghoon.jvm_jst.es.document.RegistrationDocument
import com.sanghoon.jvm_jst.es.dto.RegistrationAgg
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Registration ES Aggregation 서비스
 * pnu_id별 등기 집계 (count, lastAt, myCount, myLastAt)
 */
@Service
class RegistrationAggService(
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = RegistrationDocument.INDEX_NAME
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    /**
     * pnu_id 목록으로 등기 집계
     * @param pnuIds pnu_id 목록
     * @param userId 내 등기 집계용 (nullable)
     * @param minCreatedDate 등기 생성일 시작 (nullable)
     * @param maxCreatedDate 등기 생성일 종료 (nullable)
     * @return pnuId -> RegistrationAgg 맵
     */
    fun aggregateByPnuIds(
        pnuIds: List<String>,
        userId: Long?,
        minCreatedDate: LocalDate?,
        maxCreatedDate: LocalDate?
    ): Map<String, RegistrationAgg> {
        if (pnuIds.isEmpty()) return emptyMap()

        val startTime = System.currentTimeMillis()

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(0)
                .query { q ->
                    q.bool { bool ->
                        // pnuId IN 조건
                        bool.must { m ->
                            m.terms { t ->
                                t.field("pnuId")
                                    .terms { tv -> tv.value(pnuIds.map { FieldValue.of(it) }) }
                            }
                        }
                        // createdAt 범위 필터
                        if (minCreatedDate != null) {
                            bool.filter { f ->
                                f.range { r ->
                                    r.date { d -> d.field("createdAt").gte(minCreatedDate.toString()) }
                                }
                            }
                        }
                        if (maxCreatedDate != null) {
                            bool.filter { f ->
                                f.range { r ->
                                    r.date { d -> d.field("createdAt").lte(maxCreatedDate.toString()) }
                                }
                            }
                        }
                        bool
                    }
                }
                .aggregations("by_pnu") { agg ->
                    agg.terms { t -> t.field("pnuId").size(pnuIds.size) }
                        .aggregations("lastAt") { subAgg ->
                            subAgg.max { m -> m.field("createdAt") }
                        }
                        .aggregations("my_filter") { subAgg ->
                            if (userId != null) {
                                subAgg.filter { f -> f.term { t -> t.field("userId").value(userId) } }
                                    .aggregations("myLastAt") { mySubAgg ->
                                        mySubAgg.max { m -> m.field("createdAt") }
                                    }
                            } else {
                                // userId 없으면 빈 필터 (매칭 없음)
                                subAgg.filter { f -> f.term { t -> t.field("userId").value(-1L) } }
                            }
                        }
                }
        }, Void::class.java)

        val buckets = response.aggregations()["by_pnu"]?.sterms()?.buckets()?.array() ?: emptyList()

        val result = buckets.associate { bucket ->
            val pnuId = bucket.key().stringValue()
            val count = bucket.docCount().toInt()
            val lastAtValue = bucket.aggregations()["lastAt"]?.max()?.valueAsString()
            val lastAt = parseDateTime(lastAtValue)

            val myFilter = bucket.aggregations()["my_filter"]?.filter()
            val myCount = myFilter?.docCount()?.toInt() ?: 0
            val myLastAtValue = myFilter?.aggregations()?.get("myLastAt")?.max()?.valueAsString()
            val myLastAt = parseDateTime(myLastAtValue)

            pnuId to RegistrationAgg(
                count = count,
                lastAt = lastAt,
                myCount = myCount,
                myLastAt = myLastAt
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[RegistrationAgg] pnuIds={}, results={}, userId={}, elapsed={}ms",
            pnuIds.size, result.size, userId, elapsed)

        return result
    }

    /**
     * bbox 내 등기 조회 후 pnu_id set 반환
     */
    fun findPnuIdsByBbox(
        swLng: Double,
        swLat: Double,
        neLng: Double,
        neLat: Double,
        minCreatedDate: LocalDate?,
        maxCreatedDate: LocalDate?
    ): Set<String> {
        val startTime = System.currentTimeMillis()

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(0)
                .query { q ->
                    q.bool { bool ->
                        // bbox 조건 (center 기준)
                        bool.must { m ->
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
                        // createdAt 범위 필터
                        if (minCreatedDate != null) {
                            bool.filter { f ->
                                f.range { r ->
                                    r.date { d -> d.field("createdAt").gte(minCreatedDate.toString()) }
                                }
                            }
                        }
                        if (maxCreatedDate != null) {
                            bool.filter { f ->
                                f.range { r ->
                                    r.date { d -> d.field("createdAt").lte(maxCreatedDate.toString()) }
                                }
                            }
                        }
                        bool
                    }
                }
                .aggregations("pnu_ids") { agg ->
                    agg.terms { t -> t.field("pnuId").size(10000) }
                }
        }, Void::class.java)

        val buckets = response.aggregations()["pnu_ids"]?.sterms()?.buckets()?.array() ?: emptyList()
        val pnuIds = buckets.map { it.key().stringValue() }.toSet()

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[RegistrationAgg] bbox pnuIds={}, elapsed={}ms", pnuIds.size, elapsed)

        return pnuIds
    }

    private fun parseDateTime(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(value, dateTimeFormatter)
        } catch (e: Exception) {
            try {
                // ES가 밀리초 포함 포맷으로 반환하는 경우
                LocalDateTime.parse(value.replace("Z", ""))
            } catch (e2: Exception) {
                null
            }
        }
    }
}
