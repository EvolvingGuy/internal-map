package com.datahub.geo_poc.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * LC Aggregation 요청
 */
data class LcAggRequest(
    val swLng: Double,
    val swLat: Double,
    val neLng: Double,
    val neLat: Double
)

/**
 * LC Aggregation 필터
 */
data class LcAggFilter(
    // Building
    val buildingMainPurpsCdNm: List<String>? = null,
    val buildingRegstrGbCdNm: List<String>? = null,
    val buildingPmsDayRecent5y: Boolean? = null,
    val buildingStcnsDayRecent5y: Boolean? = null,
    val buildingUseAprDayStart: Int? = null,
    val buildingUseAprDayEnd: Int? = null,
    val buildingTotAreaMin: BigDecimal? = null,
    val buildingTotAreaMax: BigDecimal? = null,
    val buildingPlatAreaMin: BigDecimal? = null,
    val buildingPlatAreaMax: BigDecimal? = null,
    val buildingArchAreaMin: BigDecimal? = null,
    val buildingArchAreaMax: BigDecimal? = null,
    // Land
    val landJiyukCd1: List<String>? = null,
    val landJimokCd: List<String>? = null,
    val landAreaMin: Double? = null,
    val landAreaMax: Double? = null,
    val landPriceMin: Long? = null,
    val landPriceMax: Long? = null,
    // Trade
    val tradeProperty: List<String>? = null,
    val tradeContractDateStart: LocalDate? = null,
    val tradeContractDateEnd: LocalDate? = null,
    val tradeEffectiveAmountMin: Long? = null,
    val tradeEffectiveAmountMax: Long? = null,
    val tradeBuildingAmountPerM2Min: BigDecimal? = null,
    val tradeBuildingAmountPerM2Max: BigDecimal? = null,
    val tradeLandAmountPerM2Min: BigDecimal? = null,
    val tradeLandAmountPerM2Max: BigDecimal? = null
) {
    fun hasAnyFilter(): Boolean {
        return !buildingMainPurpsCdNm.isNullOrEmpty() ||
            !buildingRegstrGbCdNm.isNullOrEmpty() ||
            buildingPmsDayRecent5y == true ||
            buildingStcnsDayRecent5y == true ||
            buildingUseAprDayStart != null ||
            buildingUseAprDayEnd != null ||
            buildingTotAreaMin != null ||
            buildingTotAreaMax != null ||
            buildingPlatAreaMin != null ||
            buildingPlatAreaMax != null ||
            buildingArchAreaMin != null ||
            buildingArchAreaMax != null ||
            !landJiyukCd1.isNullOrEmpty() ||
            !landJimokCd.isNullOrEmpty() ||
            landAreaMin != null ||
            landAreaMax != null ||
            landPriceMin != null ||
            landPriceMax != null ||
            !tradeProperty.isNullOrEmpty() ||
            tradeContractDateStart != null ||
            tradeContractDateEnd != null ||
            tradeEffectiveAmountMin != null ||
            tradeEffectiveAmountMax != null ||
            tradeBuildingAmountPerM2Min != null ||
            tradeBuildingAmountPerM2Max != null ||
            tradeLandAmountPerM2Min != null ||
            tradeLandAmountPerM2Max != null
    }
}

/**
 * LC Aggregation 행정구역 아이템
 */
data class LcAggRegion(
    val code: String,
    val name: String?,
    val count: Long,
    val centerLat: Double,
    val centerLng: Double
)

/**
 * LC Aggregation 응답
 */
data class LcAggResponse(
    val level: String,
    val totalCount: Long,
    val regionCount: Int,
    val regions: List<LcAggRegion>,
    val elapsedMs: Long
)

/**
 * LC Grid Aggregation 요청
 */
data class LcGridAggRequest(
    val swLng: Double,
    val swLat: Double,
    val neLng: Double,
    val neLat: Double,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val gridSize: Int = 400
)

/**
 * LC Grid 셀 아이템
 */
data class LcGridCell(
    val gridX: Int,
    val gridY: Int,
    val count: Long,
    val centerLat: Double,
    val centerLng: Double
)

/**
 * LC Grid Aggregation 응답
 */
data class LcGridAggResponse(
    val cols: Int,
    val rows: Int,
    val totalCount: Long,
    val cellCount: Int,
    val cells: List<LcGridCell>,
    val elapsedMs: Long
)
