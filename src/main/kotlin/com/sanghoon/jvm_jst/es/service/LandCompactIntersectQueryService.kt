package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.GeoShapeRelation
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.json.JsonData
import com.sanghoon.jvm_jst.es.document.LandCompactIntersectDocument
import com.sanghoon.jvm_jst.es.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Land Compact Intersect 조회 서비스
 * geo_shape intersects 쿼리로 뷰포트와 교차하는 필지 조회
 */
@Service
class LandCompactIntersectQueryService(
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LandCompactIntersectDocument.INDEX_NAME
        const val MAX_SIZE = 10000
    }

    /**
     * bbox와 교차하는 필지 조회 (geo_shape intersects)
     */
    fun findByBboxIntersects(
        swLng: Double,
        swLat: Double,
        neLng: Double,
        neLat: Double,
        filter: LcAggFilter = LcAggFilter()
    ): Map<String, LandCompactGeoData> {
        val startTime = System.currentTimeMillis()

        // envelope GeoJSON: [[left, top], [right, bottom]] = [[swLng, neLat], [neLng, swLat]]
        val envelopeJson = mapOf(
            "type" to "envelope",
            "coordinates" to listOf(listOf(swLng, neLat), listOf(neLng, swLat))
        )

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(MAX_SIZE)
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
                        applyFilters(bool, filter)
                        bool
                    }
                }
        }, Map::class.java)

        val result = response.hits().hits().mapNotNull { hit ->
            parseLandCompactGeoData(hit.source())
        }.associateBy { it.pnu }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LcIntersectQuery] intersects results={}, elapsed={}ms", result.size, elapsed)

        return result
    }

    /**
     * 카운트만 조회 (비교용)
     */
    fun countByBboxIntersects(
        swLng: Double,
        swLat: Double,
        neLng: Double,
        neLat: Double,
        filter: LcAggFilter = LcAggFilter()
    ): Long {
        val startTime = System.currentTimeMillis()

        val envelopeJson = mapOf(
            "type" to "envelope",
            "coordinates" to listOf(listOf(swLng, neLat), listOf(neLng, swLat))
        )

        val response = esClient.count { c ->
            c.index(INDEX_NAME)
                .query { q ->
                    q.bool { bool ->
                        bool.must { must ->
                            must.geoShape { geo ->
                                geo.field("land.geometry")
                                    .shape { shape ->
                                        shape.shape(JsonData.of(envelopeJson))
                                            .relation(GeoShapeRelation.Intersects)
                                    }
                            }
                        }
                        applyFilters(bool, filter)
                        bool
                    }
                }
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LcIntersectQuery] count={}, elapsed={}ms", response.count(), elapsed)

        return response.count()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLandCompactGeoData(source: Map<*, *>?): LandCompactGeoData? {
        if (source == null) return null

        val pnu = source["pnu"]?.toString() ?: return null
        val land = source["land"] as? Map<String, Any>
        val building = source["building"] as? Map<String, Any>
        val trade = source["lastRealEstateTrade"] as? Map<String, Any>

        val center = land?.get("center") as? Map<String, Any>
        val centerLat = (center?.get("lat") as? Number)?.toDouble() ?: return null
        val centerLon = (center?.get("lon") as? Number)?.toDouble() ?: return null

        val geometry = land?.get("geometry") as? Map<String, Any>

        return LandCompactGeoData(
            pnu = pnu,
            center = MarkerCenter(centerLat, centerLon),
            land = land?.let {
                MarkerLandGeo(
                    jiyukCd1 = it["jiyukCd1"]?.toString(),
                    jimokCd = it["jimokCd"]?.toString(),
                    area = (it["area"] as? Number)?.toDouble(),
                    price = (it["price"] as? Number)?.toLong(),
                    geometry = geometry
                )
            },
            building = building?.let {
                MarkerBuilding(
                    mgmBldrgstPk = it["mgmBldrgstPk"]?.toString(),
                    mainPurpsCdNm = it["mainPurpsCdNm"]?.toString(),
                    regstrGbCdNm = it["regstrGbCdNm"]?.toString(),
                    pmsDay = parseLocalDate(it["pmsDay"]?.toString()),
                    stcnsDay = parseLocalDate(it["stcnsDay"]?.toString()),
                    useAprDay = parseLocalDate(it["useAprDay"]?.toString()),
                    totArea = (it["totArea"] as? Number)?.let { n -> BigDecimal.valueOf(n.toDouble()) },
                    platArea = (it["platArea"] as? Number)?.let { n -> BigDecimal.valueOf(n.toDouble()) },
                    archArea = (it["archArea"] as? Number)?.let { n -> BigDecimal.valueOf(n.toDouble()) }
                )
            },
            trade = trade?.let {
                MarkerTrade(
                    property = it["property"]?.toString(),
                    contractDate = parseLocalDate(it["contractDate"]?.toString()),
                    effectiveAmount = (it["effectiveAmount"] as? Number)?.toLong(),
                    buildingAmountPerM2 = (it["buildingAmountPerM2"] as? Number)?.let { n -> BigDecimal.valueOf(n.toDouble()) },
                    landAmountPerM2 = (it["landAmountPerM2"] as? Number)?.let { n -> BigDecimal.valueOf(n.toDouble()) }
                )
            }
        )
    }

    private fun parseLocalDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value.take(10))
        } catch (e: Exception) {
            null
        }
    }

    private fun applyFilters(bool: BoolQuery.Builder, filter: LcAggFilter) {
        if (!filter.buildingMainPurpsCdNm.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("building.mainPurpsCdNm").terms { tv -> tv.value(filter.buildingMainPurpsCdNm.map { FieldValue.of(it) }) } } }
        }
        if (!filter.buildingRegstrGbCdNm.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("building.regstrGbCdNm").terms { tv -> tv.value(filter.buildingRegstrGbCdNm.map { FieldValue.of(it) }) } } }
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
        if (!filter.landJiyukCd1.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("land.jiyukCd1").terms { tv -> tv.value(filter.landJiyukCd1.map { FieldValue.of(it) }) } } }
        }
        if (!filter.landJimokCd.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("land.jimokCd").terms { tv -> tv.value(filter.landJimokCd.map { FieldValue.of(it) }) } } }
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
        if (!filter.tradeProperty.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("lastRealEstateTrade.property").terms { tv -> tv.value(filter.tradeProperty.map { FieldValue.of(it) }) } } }
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
}
