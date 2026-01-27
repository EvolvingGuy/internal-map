package com.datahub.geo_poc.es.service.lnbp

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket
import com.datahub.geo_poc.es.document.land.LnbpDocument
import com.datahub.geo_poc.es.service.lsrc.LsrcQueryService
import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LcAggFilter
import com.datahub.geo_poc.model.LcAggRegion
import com.datahub.geo_poc.model.LcAggResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * LNBP (Land Nested Building Point) Aggregation 서비스
 * geo_bounding_box로 bbox 필터링 (geo_shape intersects 대신)
 * 필터 없으면 LSRC(고정형 클러스터) 사용
 */
@Service
class LnbpAggregationService(
    private val esClient: ElasticsearchClient,
    private val lsrcQueryService: LsrcQueryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LnbpDocument.INDEX_NAME
        const val SD_SIZE = 20
        const val SGG_SIZE = 300
        const val EMD_SIZE = 5000
    }

    fun aggregateBySd(bbox: BBoxRequest, filter: LcAggFilter = LcAggFilter()): LcAggResponse {
        if (!filter.hasAnyFilter()) {
            return lsrcQueryService.findByBbox("SD", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat).toLcAggResponse("SD")
        }
        return aggregate("sd", SD_SIZE, bbox, filter)
    }

    fun aggregateBySgg(bbox: BBoxRequest, filter: LcAggFilter = LcAggFilter()): LcAggResponse {
        if (!filter.hasAnyFilter()) {
            return lsrcQueryService.findByBbox("SGG", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat).toLcAggResponse("SGG")
        }
        return aggregate("sgg", SGG_SIZE, bbox, filter)
    }

    fun aggregateByEmd(bbox: BBoxRequest, filter: LcAggFilter = LcAggFilter()): LcAggResponse {
        if (!filter.hasAnyFilter()) {
            return lsrcQueryService.findByBbox("EMD", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat).toLcAggResponse("EMD")
        }
        return aggregate("emd", EMD_SIZE, bbox, filter)
    }

    private fun aggregate(field: String, size: Int, bbox: BBoxRequest, filter: LcAggFilter): LcAggResponse {
        val startTime = System.currentTimeMillis()

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(0)
                .profile(true)
                .query { q ->
                    q.bool { bool ->
                        // geo_bounding_box 쿼리 (geo_shape intersects 대신)
                        bool.must { must ->
                            must.geoBoundingBox { geo ->
                                geo.field("land.center")
                                    .boundingBox { bb ->
                                        bb.tlbr { tlbr ->
                                            tlbr.topLeft { tl -> tl.latlon { ll -> ll.lat(bbox.neLat).lon(bbox.swLng) } }
                                                .bottomRight { br -> br.latlon { ll -> ll.lat(bbox.swLat).lon(bbox.neLng) } }
                                        }
                                    }
                            }
                        }
                        applyFilters(bool, filter)
                        bool
                    }
                }
                .aggregations("by_region") { agg ->
                    agg.terms { t -> t.field(field).size(size) }
                        .aggregations("center") { subAgg ->
                            subAgg.geoCentroid { gc -> gc.field("land.center") }
                        }
                }
        }, Void::class.java)

        val buckets = response.aggregations()["by_region"]?.sterms()?.buckets()?.array() ?: emptyList()

        val regions = buckets.map { bucket -> toRegion(bucket) }
        val totalCount = regions.sumOf { it.count }
        val elapsed = System.currentTimeMillis() - startTime

        logProfile(response, field)
        log.info("[LNBP Agg] field={}, regions={}, totalCount={}, hasFilter={}, elapsed={}ms",
            field, regions.size, totalCount, filter.hasAnyFilter(), elapsed)

        return LcAggResponse(
            level = field.uppercase(),
            totalCount = totalCount,
            regionCount = regions.size,
            regions = regions,
            elapsedMs = elapsed
        )
    }

    private fun applyFilters(bool: BoolQuery.Builder, filter: LcAggFilter) {
        // Building filters - nested query로 감싸서 적용
        val buildingFilters = buildBuildingNestedFilters(filter)
        if (buildingFilters.isNotEmpty()) {
            bool.filter { f ->
                f.nested { n ->
                    n.path("buildings")
                        .query { q ->
                            q.bool { nestedBool ->
                                buildingFilters.forEach { nestedBool.filter(it) }
                                nestedBool
                            }
                        }
                }
            }
        }

        // Land filters
        if (!filter.landJiyukCd1.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("land.jiyukCd1").terms { tv -> tv.value(filter.landJiyukCd1.map { co.elastic.clients.elasticsearch._types.FieldValue.of(it) }) } } }
        }
        if (!filter.landJimokCd.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("land.jimokCd").terms { tv -> tv.value(filter.landJimokCd.map { co.elastic.clients.elasticsearch._types.FieldValue.of(it) }) } } }
        }
        if (filter.landAreaMin != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("land.area").gte(filter.landAreaMin) } } }
        }
        if (filter.landAreaMax != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("land.area").lte(filter.landAreaMax) } } }
        }
        if (filter.landPriceMin != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("land.price").gte(filter.landPriceMin.toDouble()) } } }
        }
        if (filter.landPriceMax != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("land.price").lte(filter.landPriceMax.toDouble()) } } }
        }

        // Trade filters
        if (!filter.tradeProperty.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("lastRealEstateTrade.property").terms { tv -> tv.value(filter.tradeProperty.map { co.elastic.clients.elasticsearch._types.FieldValue.of(it) }) } } }
        }
        if (filter.tradeContractDateStart != null) {
            bool.filter { f -> f.range { r -> r.date { d -> d.field("lastRealEstateTrade.contractDate").gte(filter.tradeContractDateStart.toString()) } } }
        }
        if (filter.tradeContractDateEnd != null) {
            bool.filter { f -> f.range { r -> r.date { d -> d.field("lastRealEstateTrade.contractDate").lte(filter.tradeContractDateEnd.toString()) } } }
        }
        if (filter.tradeEffectiveAmountMin != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("lastRealEstateTrade.effectiveAmount").gte(filter.tradeEffectiveAmountMin.toDouble()) } } }
        }
        if (filter.tradeEffectiveAmountMax != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("lastRealEstateTrade.effectiveAmount").lte(filter.tradeEffectiveAmountMax.toDouble()) } } }
        }
        if (filter.tradeBuildingAmountPerM2Min != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("lastRealEstateTrade.buildingAmountPerM2").gte(filter.tradeBuildingAmountPerM2Min.toDouble()) } } }
        }
        if (filter.tradeBuildingAmountPerM2Max != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("lastRealEstateTrade.buildingAmountPerM2").lte(filter.tradeBuildingAmountPerM2Max.toDouble()) } } }
        }
        if (filter.tradeLandAmountPerM2Min != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("lastRealEstateTrade.landAmountPerM2").gte(filter.tradeLandAmountPerM2Min.toDouble()) } } }
        }
        if (filter.tradeLandAmountPerM2Max != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("lastRealEstateTrade.landAmountPerM2").lte(filter.tradeLandAmountPerM2Max.toDouble()) } } }
        }
    }

    private fun buildBuildingNestedFilters(filter: LcAggFilter): List<Query> {
        val queries = mutableListOf<Query>()

        if (!filter.buildingMainPurpsCdNm.isNullOrEmpty()) {
            queries.add(Query.of { q ->
                q.terms { t ->
                    t.field("buildings.mainPurpsCdNm")
                        .terms { tv -> tv.value(filter.buildingMainPurpsCdNm.map { co.elastic.clients.elasticsearch._types.FieldValue.of(it) }) }
                }
            })
        }

        if (!filter.buildingRegstrGbCdNm.isNullOrEmpty()) {
            queries.add(Query.of { q ->
                q.terms { t ->
                    t.field("buildings.regstrGbCdNm")
                        .terms { tv -> tv.value(filter.buildingRegstrGbCdNm.map { co.elastic.clients.elasticsearch._types.FieldValue.of(it) }) }
                }
            })
        }

        if (filter.buildingPmsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            queries.add(Query.of { q ->
                q.range { r -> r.date { d -> d.field("buildings.pmsDay").gte(fiveYearsAgo.toString()) } }
            })
        }

        if (filter.buildingStcnsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            queries.add(Query.of { q ->
                q.range { r -> r.date { d -> d.field("buildings.stcnsDay").gte(fiveYearsAgo.toString()) } }
            })
        }

        if (filter.buildingUseAprDayStart != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.date { d -> d.field("buildings.useAprDay").gte("${filter.buildingUseAprDayStart}-01-01") } }
            })
        }

        if (filter.buildingUseAprDayEnd != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.date { d -> d.field("buildings.useAprDay").lte("${filter.buildingUseAprDayEnd}-12-31") } }
            })
        }

        if (filter.buildingTotAreaMin != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.number { n -> n.field("buildings.totArea").gte(filter.buildingTotAreaMin.toDouble()) } }
            })
        }

        if (filter.buildingTotAreaMax != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.number { n -> n.field("buildings.totArea").lte(filter.buildingTotAreaMax.toDouble()) } }
            })
        }

        if (filter.buildingPlatAreaMin != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.number { n -> n.field("buildings.platArea").gte(filter.buildingPlatAreaMin.toDouble()) } }
            })
        }

        if (filter.buildingPlatAreaMax != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.number { n -> n.field("buildings.platArea").lte(filter.buildingPlatAreaMax.toDouble()) } }
            })
        }

        if (filter.buildingArchAreaMin != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.number { n -> n.field("buildings.archArea").gte(filter.buildingArchAreaMin.toDouble()) } }
            })
        }

        if (filter.buildingArchAreaMax != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.number { n -> n.field("buildings.archArea").lte(filter.buildingArchAreaMax.toDouble()) } }
            })
        }

        return queries
    }

    private fun toRegion(bucket: StringTermsBucket): LcAggRegion {
        val code = bucket.key().stringValue()
        val count = bucket.docCount()
        val centroid = bucket.aggregations()["center"]?.geoCentroid()?.location()

        return LcAggRegion(
            code = code,
            name = null,
            count = count,
            centerLat = centroid?.latlon()?.lat() ?: 0.0,
            centerLng = centroid?.latlon()?.lon() ?: 0.0
        )
    }

    private fun logProfile(response: co.elastic.clients.elasticsearch.core.SearchResponse<Void>, field: String) {
        val profile = response.profile() ?: return
        val shards = profile.shards()

        shards.forEachIndexed { idx, shardProfile ->
            shardProfile.searches().forEach { search ->
                search.query().forEach { queryProfile ->
                    val timeMs = String.format("%.2f", queryProfile.timeInNanos() / 1_000_000.0)
                    log.info("[LNBP Agg Profile] field={}, 샤드[{}] {} - {}ms", field, idx, queryProfile.type(), timeMs)
                }
            }
        }
    }
}
