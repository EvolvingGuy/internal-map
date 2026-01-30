package com.datahub.geo_poc.es.service.lc

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.GeoShapeRelation
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.json.JsonData
import com.datahub.geo_poc.es.document.land.LandCompactDocument
import com.datahub.geo_poc.es.model.LandCompactData
import com.datahub.geo_poc.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Land Compact ES 조회 서비스
 * geo_shape intersects 쿼리로 뷰포트와 교차하는 필지 조회
 */
@Service
class LandCompactQueryService(
    private val esClient: OpenSearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LandCompactDocument.INDEX_NAME
        const val MAX_SIZE = 10000
    }

    /**
     * bbox와 교차하는 필지 조회 (geo_shape intersects)
     * @return pnu -> LandCompactData 맵
     */
    fun findByBbox(
        swLng: Double,
        swLat: Double,
        neLng: Double,
        neLat: Double,
        filter: LcAggFilter = LcAggFilter()
    ): Map<String, LandCompactData> {
        val startTime = System.currentTimeMillis()

        // envelope GeoJSON: [[left, top], [right, bottom]] = [[swLng, neLat], [neLng, swLat]]
        val envelopeJson = mapOf(
            "type" to "envelope",
            "coordinates" to listOf(listOf(swLng, neLat), listOf(neLng, swLat))
        )

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(MAX_SIZE)
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
                        applyFilters(bool, filter)
                        bool
                    }
                }
        }, Map::class.java)
        logProfile(response, "bbox")

        val result = response.hits().hits().mapNotNull { hit ->
            parseLandCompactData(hit.source())
        }.associateBy { it.pnu }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LcQuery] bbox intersects results={}, hasFilter={}, elapsed={}ms",
            result.size, filter.hasAnyFilter(), elapsed)

        return result
    }

    /**
     * pnu 목록으로 land_compact 조회
     * @return pnu -> LandCompactData 맵
     */
    fun findByPnuIds(
        pnuIds: Collection<String>,
        filter: LcAggFilter = LcAggFilter()
    ): Map<String, LandCompactData> {
        if (pnuIds.isEmpty()) return emptyMap()

        val startTime = System.currentTimeMillis()

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(pnuIds.size.coerceAtMost(MAX_SIZE))
                .profile(true)
                .query { q ->
                    q.bool { bool ->
                        bool.must { m ->
                            m.terms { t ->
                                t.field("pnu")
                                    .terms { tv -> tv.value(pnuIds.map { FieldValue.of(it) }) }
                            }
                        }
                        applyFilters(bool, filter)
                        bool
                    }
                }
        }, Map::class.java)
        logProfile(response, "pnuIds")

        val result = response.hits().hits().mapNotNull { hit ->
            parseLandCompactData(hit.source())
        }.associateBy { it.pnu }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LcQuery] pnuIds={}, results={}, hasFilter={}, elapsed={}ms",
            pnuIds.size, result.size, filter.hasAnyFilter(), elapsed)

        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLandCompactData(source: Map<*, *>?): LandCompactData? {
        if (source == null) return null

        val pnu = source["pnu"]?.toString() ?: return null
        val land = source["land"] as? Map<String, Any>
        val building = source["building"] as? Map<String, Any>
        val trade = source["lastRealEstateTrade"] as? Map<String, Any>

        val center = land?.get("center") as? Map<String, Any>
        val centerLat = (center?.get("lat") as? Number)?.toDouble() ?: return null
        val centerLon = (center?.get("lon") as? Number)?.toDouble() ?: return null

        val geometry = land?.get("geometry") as? Map<String, Any>

        return LandCompactData(
            pnu = pnu,
            center = MarkerCenter(centerLat, centerLon),
            land = land?.let {
                MarkerLand(
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
        // Building filters
        if (!filter.buildingMainPurpsCdNm.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("building.mainPurpsCdNm").terms { tv -> tv.value(filter.buildingMainPurpsCdNm.map { FieldValue.of(it) }) } } }
        }
        if (!filter.buildingRegstrGbCdNm.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("building.regstrGbCdNm").terms { tv -> tv.value(filter.buildingRegstrGbCdNm.map { FieldValue.of(it) }) } } }
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
            bool.filter { f -> f.terms { t -> t.field("land.jiyukCd1").terms { tv -> tv.value(filter.landJiyukCd1.map { FieldValue.of(it) }) } } }
        }
        if (!filter.landJimokCd.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("land.jimokCd").terms { tv -> tv.value(filter.landJimokCd.map { FieldValue.of(it) }) } } }
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
            bool.filter { f -> f.terms { t -> t.field("lastRealEstateTrade.property").terms { tv -> tv.value(filter.tradeProperty.map { FieldValue.of(it) }) } } }
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

    private fun logProfile(response: org.opensearch.client.opensearch.core.SearchResponse<Map<*, *>>, queryType: String) {
        val profile = response.profile() ?: return
        val shards = profile.shards()

        shards.forEachIndexed { idx, shardProfile ->
            shardProfile.searches().forEach { search ->
                search.query().forEach { queryProfile ->
                    val timeMs = String.format("%.2f", queryProfile.timeInNanos() / 1_000_000.0)
                    log.info("[LcQuery Profile] type={}, 샤드[{}] {} - {}ms", queryType, idx, queryProfile.type(), timeMs)
                }
            }
        }
    }
}
