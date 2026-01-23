package com.datahub.geo_poc.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.GeoShapeRelation
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket
import co.elastic.clients.json.JsonData
import com.datahub.geo_poc.es.document.land.LandCompactDocument
import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LcAggFilter
import com.datahub.geo_poc.model.LcAggRegion
import com.datahub.geo_poc.model.LcAggResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * LC (Land Compact) Aggregation 서비스
 * land-compact 인덱스에서 행정구역별 aggregation
 */
@Service
class LcAggregationService(
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LandCompactDocument.INDEX_NAME
        const val SD_SIZE = 20      // 시도 최대 개수
        const val SGG_SIZE = 300    // 시군구 최대 개수
        const val EMD_SIZE = 5000   // 읍면동 최대 개수
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

        // envelope GeoJSON: [[left, top], [right, bottom]]
        val envelopeJson = mapOf(
            "type" to "envelope",
            "coordinates" to listOf(listOf(bbox.swLng, bbox.neLat), listOf(bbox.neLng, bbox.swLat))
        )

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(0)  // hits 불필요
                .profile(true)
                .query { q ->
                    q.bool { bool ->
                        // geo_shape intersects 쿼리
                        bool.must { must ->
                            must.geoShape { geo ->
                                geo.field("land.geometry")
                                    .shape { shape ->
                                        shape.shape(JsonData.of(envelopeJson))
                                            .relation(GeoShapeRelation.Intersects)
                                    }
                            }
                        }
                        // 필터 적용
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
        logProfile(response, field)

        val buckets = response.aggregations()["by_region"]?.sterms()?.buckets()?.array() ?: emptyList()

        val regions = buckets.map { bucket -> toRegion(bucket) }
        val totalCount = regions.sumOf { it.count }
        val elapsed = System.currentTimeMillis() - startTime

        log.info("[LC Agg] field={}, regions={}, totalCount={}, hasFilter={}, elapsed={}ms",
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
            bool.filter { f -> f.terms { t -> t.field("building.mainPurpsCdNm").terms { tv -> tv.value(filter.buildingMainPurpsCdNm.map { co.elastic.clients.elasticsearch._types.FieldValue.of(it) }) } } }
        }
        if (!filter.buildingRegstrGbCdNm.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("building.regstrGbCdNm").terms { tv -> tv.value(filter.buildingRegstrGbCdNm.map { co.elastic.clients.elasticsearch._types.FieldValue.of(it) }) } } }
        }
        if (filter.buildingPmsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            bool.filter { f -> f.range { r -> r.date { d -> d.field("building.pmsDay").gte(fiveYearsAgo.toString()) } } }
        }
        if (filter.buildingStcnsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            bool.filter { f -> f.range { r -> r.date { d -> d.field("building.stcnsDay").gte(fiveYearsAgo.toString()) } } }
        }
        if (filter.buildingUseAprDayStart != null) {
            bool.filter { f -> f.range { r -> r.date { d -> d.field("building.useAprDay").gte("${filter.buildingUseAprDayStart}-01-01") } } }
        }
        if (filter.buildingUseAprDayEnd != null) {
            bool.filter { f -> f.range { r -> r.date { d -> d.field("building.useAprDay").lte("${filter.buildingUseAprDayEnd}-12-31") } } }
        }
        if (filter.buildingTotAreaMin != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("building.totArea").gte(filter.buildingTotAreaMin.toDouble()) } } }
        }
        if (filter.buildingTotAreaMax != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("building.totArea").lte(filter.buildingTotAreaMax.toDouble()) } } }
        }
        if (filter.buildingPlatAreaMin != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("building.platArea").gte(filter.buildingPlatAreaMin.toDouble()) } } }
        }
        if (filter.buildingPlatAreaMax != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("building.platArea").lte(filter.buildingPlatAreaMax.toDouble()) } } }
        }
        if (filter.buildingArchAreaMin != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("building.archArea").gte(filter.buildingArchAreaMin.toDouble()) } } }
        }
        if (filter.buildingArchAreaMax != null) {
            bool.filter { f -> f.range { r -> r.number { n -> n.field("building.archArea").lte(filter.buildingArchAreaMax.toDouble()) } } }
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

    private fun toRegion(bucket: StringTermsBucket): LcAggRegion {
        val code = bucket.key().stringValue()
        val count = bucket.docCount()
        val centroid = bucket.aggregations()["center"]?.geoCentroid()?.location()

        return LcAggRegion(
            code = code,
            name = null,  // 추후 행정구역 이름 매핑
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
                    log.info("[LC Agg Profile] field={}, 샤드[{}] {} - {}ms", field, idx, queryProfile.type(), timeMs)
                }
            }
        }
    }
}
