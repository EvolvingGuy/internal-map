package com.datahub.geo_poc.es.service.lcp

import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket
import com.datahub.geo_poc.es.document.land.LcpDocument
import com.datahub.geo_poc.es.service.lsrc.LsrcQueryService
import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LcAggFilter
import com.datahub.geo_poc.model.LcAggRegion
import com.datahub.geo_poc.model.LcAggResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * LCP (Land Compact Point) Aggregation 서비스
 * geo_bounding_box로 bbox 필터링 (geo_shape intersects 대신)
 * 필터 없으면 LSRC(고정형 클러스터) 사용
 */
@Service
class LcpAggregationService(
    private val esClient: OpenSearchClient,
    private val lsrcQueryService: LsrcQueryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LcpDocument.INDEX_NAME
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
                        // geo_bounding_box 쿼리 (LC의 geo_shape intersects 대신)
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
        log.info("[LCP Agg] field={}, regions={}, totalCount={}, hasFilter={}, elapsed={}ms",
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
        // Building filters
        if (!filter.buildingMainPurpsCdNm.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("building.mainPurpsCdNm").terms { tv -> tv.value(filter.buildingMainPurpsCdNm.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } }
        }
        if (!filter.buildingRegstrGbCdNm.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("building.regstrGbCdNm").terms { tv -> tv.value(filter.buildingRegstrGbCdNm.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } }
        }
        if (filter.buildingPmsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            bool.filter { f -> f.range { r -> r.field("building.pmsDay").gte(JsonData.of(fiveYearsAgo.toString())) } }
        }
        if (filter.buildingStcnsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            bool.filter { f -> f.range { r -> r.field("building.stcnsDay").gte(JsonData.of(fiveYearsAgo.toString())) } }
        }
        if (filter.buildingUseAprDayStart != null) {
            bool.filter { f -> f.range { r -> r.field("building.useAprDay").gte(JsonData.of("${filter.buildingUseAprDayStart}-01-01")) } }
        }
        if (filter.buildingUseAprDayEnd != null) {
            bool.filter { f -> f.range { r -> r.field("building.useAprDay").lte(JsonData.of("${filter.buildingUseAprDayEnd}-12-31")) } }
        }
        if (filter.buildingTotAreaMin != null) {
            bool.filter { f -> f.range { r -> r.field("building.totArea").gte(JsonData.of(filter.buildingTotAreaMin.toDouble())) } }
        }
        if (filter.buildingTotAreaMax != null) {
            bool.filter { f -> f.range { r -> r.field("building.totArea").lte(JsonData.of(filter.buildingTotAreaMax.toDouble())) } }
        }
        if (filter.buildingPlatAreaMin != null) {
            bool.filter { f -> f.range { r -> r.field("building.platArea").gte(JsonData.of(filter.buildingPlatAreaMin.toDouble())) } }
        }
        if (filter.buildingPlatAreaMax != null) {
            bool.filter { f -> f.range { r -> r.field("building.platArea").lte(JsonData.of(filter.buildingPlatAreaMax.toDouble())) } }
        }
        if (filter.buildingArchAreaMin != null) {
            bool.filter { f -> f.range { r -> r.field("building.archArea").gte(JsonData.of(filter.buildingArchAreaMin.toDouble())) } }
        }
        if (filter.buildingArchAreaMax != null) {
            bool.filter { f -> f.range { r -> r.field("building.archArea").lte(JsonData.of(filter.buildingArchAreaMax.toDouble())) } }
        }

        // Land filters
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

        // Trade filters
        if (!filter.tradeProperty.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("lastRealEstateTrade.property").terms { tv -> tv.value(filter.tradeProperty.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } }
        }
        if (filter.tradeContractDateStart != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.contractDate").gte(JsonData.of(filter.tradeContractDateStart.toString())) } }
        }
        if (filter.tradeContractDateEnd != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.contractDate").lte(JsonData.of(filter.tradeContractDateEnd.toString())) } }
        }
        if (filter.tradeEffectiveAmountMin != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.effectiveAmount").gte(JsonData.of(filter.tradeEffectiveAmountMin.toDouble())) } }
        }
        if (filter.tradeEffectiveAmountMax != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.effectiveAmount").lte(JsonData.of(filter.tradeEffectiveAmountMax.toDouble())) } }
        }
        if (filter.tradeBuildingAmountPerM2Min != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.buildingAmountPerM2").gte(JsonData.of(filter.tradeBuildingAmountPerM2Min.toDouble())) } }
        }
        if (filter.tradeBuildingAmountPerM2Max != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.buildingAmountPerM2").lte(JsonData.of(filter.tradeBuildingAmountPerM2Max.toDouble())) } }
        }
        if (filter.tradeLandAmountPerM2Min != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.landAmountPerM2").gte(JsonData.of(filter.tradeLandAmountPerM2Min.toDouble())) } }
        }
        if (filter.tradeLandAmountPerM2Max != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.landAmountPerM2").lte(JsonData.of(filter.tradeLandAmountPerM2Max.toDouble())) } }
        }
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

    private fun logProfile(response: org.opensearch.client.opensearch.core.SearchResponse<Void>, field: String) {
        val profile = response.profile() ?: return
        val shards = profile.shards()

        shards.forEachIndexed { idx, shardProfile ->
            shardProfile.searches().forEach { search ->
                search.query().forEach { queryProfile ->
                    val timeMs = String.format("%.2f", queryProfile.timeInNanos() / 1_000_000.0)
                    log.info("[LCP Agg Profile] field={}, 샤드[{}] {} - {}ms", field, idx, queryProfile.type(), timeMs)
                }
            }
        }
    }
}
