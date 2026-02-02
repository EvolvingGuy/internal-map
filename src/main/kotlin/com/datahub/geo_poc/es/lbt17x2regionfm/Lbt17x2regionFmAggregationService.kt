package com.datahub.geo_poc.es.lbt17x2regionfm

import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket
import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LcAggFilter
import com.datahub.geo_poc.model.LcAggRegion
import com.datahub.geo_poc.model.LcAggResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * LBT_17x2_REGION_FM Aggregation 서비스
 * 와일드카드 패턴으로 17개 인덱스 동시 조회
 */
@Service
class Lbt17x2regionFmAggregationService(
    @Value("\${opensearch.profile.enabled:false}") private val profileEnabled: Boolean,
    private val esClient: OpenSearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        val INDEX_PATTERN = Lbt17x2regionFmDocument.allIndexPattern()
        const val SD_SIZE = 20
        const val SGG_SIZE = 300
        const val EMD_SIZE = 5000
    }

    fun aggregateBySd(bbox: BBoxRequest, filter: LcAggFilter = LcAggFilter()): LcAggResponse {
        return aggregate("sd", SD_SIZE, bbox, filter)
    }

    fun aggregateBySgg(bbox: BBoxRequest, filter: LcAggFilter = LcAggFilter()): LcAggResponse {
        return aggregate("sgg", SGG_SIZE, bbox, filter)
    }

    fun aggregateByEmd(bbox: BBoxRequest, filter: LcAggFilter = LcAggFilter()): LcAggResponse {
        return aggregate("emd", EMD_SIZE, bbox, filter)
    }

    private fun aggregate(field: String, size: Int, bbox: BBoxRequest, filter: LcAggFilter): LcAggResponse {
        val startTime = System.currentTimeMillis()

        val response = esClient.search({ s ->
            s.index(INDEX_PATTERN)
                .size(0)
                .profile(profileEnabled)
                .query { q ->
                    q.bool { bool ->
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

        log.info("[LBT_17x2_REGION_FM Agg] field={}, regions={}, totalCount={}, hasFilter={}, index={}, elapsed={}ms",
            field, regions.size, totalCount, filter.hasAnyFilter(), INDEX_PATTERN, elapsed)

        return LcAggResponse(
            level = field.uppercase(),
            totalCount = totalCount,
            regionCount = regions.size,
            regions = regions,
            elapsedMs = elapsed
        )
    }

    private fun applyFilters(bool: BoolQuery.Builder, filter: LcAggFilter) {
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

        if (!filter.landJiyukCd1.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("land.jiyukCd1").terms { tv -> tv.value(filter.landJiyukCd1.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } }
        }
        if (!filter.landJimokCd.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("land.jimokCd").terms { tv -> tv.value(filter.landJimokCd.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } }
        }
        if (filter.landAreaMin != null) {
            bool.filter { f -> f.range { r -> r.field("land.area").gte(JsonData.of(filter.landAreaMin)) } }
        }
        if (filter.landAreaMax != null) {
            bool.filter { f -> f.range { r -> r.field("land.area").lte(JsonData.of(filter.landAreaMax)) } }
        }
        if (filter.landPriceMin != null) {
            bool.filter { f -> f.range { r -> r.field("land.price").gte(JsonData.of(filter.landPriceMin.toDouble())) } }
        }
        if (filter.landPriceMax != null) {
            bool.filter { f -> f.range { r -> r.field("land.price").lte(JsonData.of(filter.landPriceMax.toDouble())) } }
        }

        val tradeFilters = buildTradeNestedFilters(filter)
        if (tradeFilters.isNotEmpty()) {
            bool.filter { f ->
                f.nested { n ->
                    n.path("trades")
                        .query { q ->
                            q.bool { nestedBool ->
                                tradeFilters.forEach { nestedBool.filter(it) }
                                nestedBool
                            }
                        }
                }
            }
        }
    }

    private fun buildBuildingNestedFilters(filter: LcAggFilter): List<Query> {
        val queries = mutableListOf<Query>()

        if (!filter.buildingMainPurpsCdNm.isNullOrEmpty()) {
            queries.add(Query.of { q -> q.terms { t -> t.field("buildings.mainPurpsCdNm").terms { tv -> tv.value(filter.buildingMainPurpsCdNm.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } })
        }
        if (!filter.buildingRegstrGbCdNm.isNullOrEmpty()) {
            queries.add(Query.of { q -> q.terms { t -> t.field("buildings.regstrGbCdNm").terms { tv -> tv.value(filter.buildingRegstrGbCdNm.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } })
        }
        if (filter.buildingPmsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            queries.add(Query.of { q -> q.range { r -> r.field("buildings.pmsDay").gte(JsonData.of(fiveYearsAgo.toString())) } })
        }
        if (filter.buildingStcnsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            queries.add(Query.of { q -> q.range { r -> r.field("buildings.stcnsDay").gte(JsonData.of(fiveYearsAgo.toString())) } })
        }
        if (filter.buildingUseAprDayStart != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("buildings.useAprDay").gte(JsonData.of("${filter.buildingUseAprDayStart}-01-01")) } })
        }
        if (filter.buildingUseAprDayEnd != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("buildings.useAprDay").lte(JsonData.of("${filter.buildingUseAprDayEnd}-12-31")) } })
        }
        if (filter.buildingTotAreaMin != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("buildings.totArea").gte(JsonData.of(filter.buildingTotAreaMin.toDouble())) } })
        }
        if (filter.buildingTotAreaMax != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("buildings.totArea").lte(JsonData.of(filter.buildingTotAreaMax.toDouble())) } })
        }
        if (filter.buildingPlatAreaMin != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("buildings.platArea").gte(JsonData.of(filter.buildingPlatAreaMin.toDouble())) } })
        }
        if (filter.buildingPlatAreaMax != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("buildings.platArea").lte(JsonData.of(filter.buildingPlatAreaMax.toDouble())) } })
        }
        if (filter.buildingArchAreaMin != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("buildings.archArea").gte(JsonData.of(filter.buildingArchAreaMin.toDouble())) } })
        }
        if (filter.buildingArchAreaMax != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("buildings.archArea").lte(JsonData.of(filter.buildingArchAreaMax.toDouble())) } })
        }

        return queries
    }

    private fun buildTradeNestedFilters(filter: LcAggFilter): List<Query> {
        val queries = mutableListOf<Query>()

        if (!filter.tradeProperty.isNullOrEmpty()) {
            queries.add(Query.of { q -> q.terms { t -> t.field("trades.property").terms { tv -> tv.value(filter.tradeProperty.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } })
        }
        if (filter.tradeContractDateStart != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("trades.contractDate").gte(JsonData.of(filter.tradeContractDateStart.toString())) } })
        }
        if (filter.tradeContractDateEnd != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("trades.contractDate").lte(JsonData.of(filter.tradeContractDateEnd.toString())) } })
        }
        if (filter.tradeEffectiveAmountMin != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("trades.effectiveAmount").gte(JsonData.of(filter.tradeEffectiveAmountMin.toDouble())) } })
        }
        if (filter.tradeEffectiveAmountMax != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("trades.effectiveAmount").lte(JsonData.of(filter.tradeEffectiveAmountMax.toDouble())) } })
        }
        if (filter.tradeBuildingAmountPerM2Min != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("trades.buildingAmountPerM2").gte(JsonData.of(filter.tradeBuildingAmountPerM2Min.toDouble())) } })
        }
        if (filter.tradeBuildingAmountPerM2Max != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("trades.buildingAmountPerM2").lte(JsonData.of(filter.tradeBuildingAmountPerM2Max.toDouble())) } })
        }
        if (filter.tradeLandAmountPerM2Min != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("trades.landAmountPerM2").gte(JsonData.of(filter.tradeLandAmountPerM2Min.toDouble())) } })
        }
        if (filter.tradeLandAmountPerM2Max != null) {
            queries.add(Query.of { q -> q.range { r -> r.field("trades.landAmountPerM2").lte(JsonData.of(filter.tradeLandAmountPerM2Max.toDouble())) } })
        }

        return queries
    }

    private fun toRegion(bucket: StringTermsBucket): LcAggRegion {
        val code = bucket.key()
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
}
