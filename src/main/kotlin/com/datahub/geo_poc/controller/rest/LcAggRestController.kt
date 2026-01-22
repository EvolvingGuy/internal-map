package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.model.*
import com.datahub.geo_poc.es.service.LcAggregationService
import com.datahub.geo_poc.es.service.LcGridAggregationService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * LC Aggregation API Controller
 */
@RestController
@RequestMapping("/api/es/lc/agg")
class LcAggRestController(
    private val aggService: LcAggregationService,
    private val gridAggService: LcGridAggregationService
) {
    @GetMapping("/sd")
    fun aggregateSd(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        // Building filters
        @RequestParam(required = false) buildingMainPurpsCdNm: String?,
        @RequestParam(required = false) buildingRegstrGbCdNm: String?,
        @RequestParam(required = false) buildingPmsDayRecent5y: Boolean?,
        @RequestParam(required = false) buildingStcnsDayRecent5y: Boolean?,
        @RequestParam(required = false) buildingUseAprDayStart: Int?,
        @RequestParam(required = false) buildingUseAprDayEnd: Int?,
        @RequestParam(required = false) buildingTotAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingTotAreaMax: BigDecimal?,
        @RequestParam(required = false) buildingPlatAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingPlatAreaMax: BigDecimal?,
        @RequestParam(required = false) buildingArchAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingArchAreaMax: BigDecimal?,
        // Land filters
        @RequestParam(required = false) landJiyukCd1: String?,
        @RequestParam(required = false) landJimokCd: String?,
        @RequestParam(required = false) landAreaMin: Double?,
        @RequestParam(required = false) landAreaMax: Double?,
        @RequestParam(required = false) landPriceMin: Long?,
        @RequestParam(required = false) landPriceMax: Long?,
        // Trade filters
        @RequestParam(required = false) tradeProperty: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) tradeContractDateStart: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) tradeContractDateEnd: LocalDate?,
        @RequestParam(required = false) tradeEffectiveAmountMin: Long?,
        @RequestParam(required = false) tradeEffectiveAmountMax: Long?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Max: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Max: BigDecimal?
    ): ResponseEntity<LcAggResponse> {
        val request = LcAggRequest(swLng, swLat, neLng, neLat)
        val filter = buildFilter(
            buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingPmsDayRecent5y, buildingStcnsDayRecent5y,
            buildingUseAprDayStart, buildingUseAprDayEnd, buildingTotAreaMin, buildingTotAreaMax,
            buildingPlatAreaMin, buildingPlatAreaMax, buildingArchAreaMin, buildingArchAreaMax,
            landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax,
            tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax,
            tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max, tradeLandAmountPerM2Min, tradeLandAmountPerM2Max
        )
        return ResponseEntity.ok(aggService.aggregateBySd(request, filter))
    }

    @GetMapping("/sgg")
    fun aggregateSgg(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        // Building filters
        @RequestParam(required = false) buildingMainPurpsCdNm: String?,
        @RequestParam(required = false) buildingRegstrGbCdNm: String?,
        @RequestParam(required = false) buildingPmsDayRecent5y: Boolean?,
        @RequestParam(required = false) buildingStcnsDayRecent5y: Boolean?,
        @RequestParam(required = false) buildingUseAprDayStart: Int?,
        @RequestParam(required = false) buildingUseAprDayEnd: Int?,
        @RequestParam(required = false) buildingTotAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingTotAreaMax: BigDecimal?,
        @RequestParam(required = false) buildingPlatAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingPlatAreaMax: BigDecimal?,
        @RequestParam(required = false) buildingArchAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingArchAreaMax: BigDecimal?,
        // Land filters
        @RequestParam(required = false) landJiyukCd1: String?,
        @RequestParam(required = false) landJimokCd: String?,
        @RequestParam(required = false) landAreaMin: Double?,
        @RequestParam(required = false) landAreaMax: Double?,
        @RequestParam(required = false) landPriceMin: Long?,
        @RequestParam(required = false) landPriceMax: Long?,
        // Trade filters
        @RequestParam(required = false) tradeProperty: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) tradeContractDateStart: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) tradeContractDateEnd: LocalDate?,
        @RequestParam(required = false) tradeEffectiveAmountMin: Long?,
        @RequestParam(required = false) tradeEffectiveAmountMax: Long?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Max: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Max: BigDecimal?
    ): ResponseEntity<LcAggResponse> {
        val request = LcAggRequest(swLng, swLat, neLng, neLat)
        val filter = buildFilter(
            buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingPmsDayRecent5y, buildingStcnsDayRecent5y,
            buildingUseAprDayStart, buildingUseAprDayEnd, buildingTotAreaMin, buildingTotAreaMax,
            buildingPlatAreaMin, buildingPlatAreaMax, buildingArchAreaMin, buildingArchAreaMax,
            landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax,
            tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax,
            tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max, tradeLandAmountPerM2Min, tradeLandAmountPerM2Max
        )
        return ResponseEntity.ok(aggService.aggregateBySgg(request, filter))
    }

    @GetMapping("/emd")
    fun aggregateEmd(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        // Building filters
        @RequestParam(required = false) buildingMainPurpsCdNm: String?,
        @RequestParam(required = false) buildingRegstrGbCdNm: String?,
        @RequestParam(required = false) buildingPmsDayRecent5y: Boolean?,
        @RequestParam(required = false) buildingStcnsDayRecent5y: Boolean?,
        @RequestParam(required = false) buildingUseAprDayStart: Int?,
        @RequestParam(required = false) buildingUseAprDayEnd: Int?,
        @RequestParam(required = false) buildingTotAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingTotAreaMax: BigDecimal?,
        @RequestParam(required = false) buildingPlatAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingPlatAreaMax: BigDecimal?,
        @RequestParam(required = false) buildingArchAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingArchAreaMax: BigDecimal?,
        // Land filters
        @RequestParam(required = false) landJiyukCd1: String?,
        @RequestParam(required = false) landJimokCd: String?,
        @RequestParam(required = false) landAreaMin: Double?,
        @RequestParam(required = false) landAreaMax: Double?,
        @RequestParam(required = false) landPriceMin: Long?,
        @RequestParam(required = false) landPriceMax: Long?,
        // Trade filters
        @RequestParam(required = false) tradeProperty: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) tradeContractDateStart: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) tradeContractDateEnd: LocalDate?,
        @RequestParam(required = false) tradeEffectiveAmountMin: Long?,
        @RequestParam(required = false) tradeEffectiveAmountMax: Long?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Max: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Max: BigDecimal?
    ): ResponseEntity<LcAggResponse> {
        val request = LcAggRequest(swLng, swLat, neLng, neLat)
        val filter = buildFilter(
            buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingPmsDayRecent5y, buildingStcnsDayRecent5y,
            buildingUseAprDayStart, buildingUseAprDayEnd, buildingTotAreaMin, buildingTotAreaMax,
            buildingPlatAreaMin, buildingPlatAreaMax, buildingArchAreaMin, buildingArchAreaMax,
            landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax,
            tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax,
            tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max, tradeLandAmountPerM2Min, tradeLandAmountPerM2Max
        )
        return ResponseEntity.ok(aggService.aggregateByEmd(request, filter))
    }

    @GetMapping("/grid")
    fun aggregateGrid(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        @RequestParam viewportWidth: Int,
        @RequestParam viewportHeight: Int,
        @RequestParam(required = false, defaultValue = "400") gridSize: Int,
        // Building filters
        @RequestParam(required = false) buildingMainPurpsCdNm: String?,
        @RequestParam(required = false) buildingRegstrGbCdNm: String?,
        @RequestParam(required = false) buildingPmsDayRecent5y: Boolean?,
        @RequestParam(required = false) buildingStcnsDayRecent5y: Boolean?,
        @RequestParam(required = false) buildingUseAprDayStart: Int?,
        @RequestParam(required = false) buildingUseAprDayEnd: Int?,
        @RequestParam(required = false) buildingTotAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingTotAreaMax: BigDecimal?,
        @RequestParam(required = false) buildingPlatAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingPlatAreaMax: BigDecimal?,
        @RequestParam(required = false) buildingArchAreaMin: BigDecimal?,
        @RequestParam(required = false) buildingArchAreaMax: BigDecimal?,
        // Land filters
        @RequestParam(required = false) landJiyukCd1: String?,
        @RequestParam(required = false) landJimokCd: String?,
        @RequestParam(required = false) landAreaMin: Double?,
        @RequestParam(required = false) landAreaMax: Double?,
        @RequestParam(required = false) landPriceMin: Long?,
        @RequestParam(required = false) landPriceMax: Long?,
        // Trade filters
        @RequestParam(required = false) tradeProperty: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) tradeContractDateStart: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) tradeContractDateEnd: LocalDate?,
        @RequestParam(required = false) tradeEffectiveAmountMin: Long?,
        @RequestParam(required = false) tradeEffectiveAmountMax: Long?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Max: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Max: BigDecimal?
    ): ResponseEntity<LcGridAggResponse> {
        val request = LcGridAggRequest(swLng, swLat, neLng, neLat, viewportWidth, viewportHeight, gridSize)
        val filter = buildFilter(
            buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingPmsDayRecent5y, buildingStcnsDayRecent5y,
            buildingUseAprDayStart, buildingUseAprDayEnd, buildingTotAreaMin, buildingTotAreaMax,
            buildingPlatAreaMin, buildingPlatAreaMax, buildingArchAreaMin, buildingArchAreaMax,
            landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax,
            tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax,
            tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max, tradeLandAmountPerM2Min, tradeLandAmountPerM2Max
        )
        return ResponseEntity.ok(gridAggService.aggregate(request, filter))
    }

    private fun buildFilter(
        buildingMainPurpsCdNm: String?,
        buildingRegstrGbCdNm: String?,
        buildingPmsDayRecent5y: Boolean?,
        buildingStcnsDayRecent5y: Boolean?,
        buildingUseAprDayStart: Int?,
        buildingUseAprDayEnd: Int?,
        buildingTotAreaMin: BigDecimal?,
        buildingTotAreaMax: BigDecimal?,
        buildingPlatAreaMin: BigDecimal?,
        buildingPlatAreaMax: BigDecimal?,
        buildingArchAreaMin: BigDecimal?,
        buildingArchAreaMax: BigDecimal?,
        landJiyukCd1: String?,
        landJimokCd: String?,
        landAreaMin: Double?,
        landAreaMax: Double?,
        landPriceMin: Long?,
        landPriceMax: Long?,
        tradeProperty: String?,
        tradeContractDateStart: LocalDate?,
        tradeContractDateEnd: LocalDate?,
        tradeEffectiveAmountMin: Long?,
        tradeEffectiveAmountMax: Long?,
        tradeBuildingAmountPerM2Min: BigDecimal?,
        tradeBuildingAmountPerM2Max: BigDecimal?,
        tradeLandAmountPerM2Min: BigDecimal?,
        tradeLandAmountPerM2Max: BigDecimal?
    ): LcAggFilter {
        return LcAggFilter(
            buildingMainPurpsCdNm = buildingMainPurpsCdNm?.split(",")?.filter { it.isNotBlank() },
            buildingRegstrGbCdNm = buildingRegstrGbCdNm?.split(",")?.filter { it.isNotBlank() },
            buildingPmsDayRecent5y = buildingPmsDayRecent5y,
            buildingStcnsDayRecent5y = buildingStcnsDayRecent5y,
            buildingUseAprDayStart = buildingUseAprDayStart,
            buildingUseAprDayEnd = buildingUseAprDayEnd,
            buildingTotAreaMin = buildingTotAreaMin,
            buildingTotAreaMax = buildingTotAreaMax,
            buildingPlatAreaMin = buildingPlatAreaMin,
            buildingPlatAreaMax = buildingPlatAreaMax,
            buildingArchAreaMin = buildingArchAreaMin,
            buildingArchAreaMax = buildingArchAreaMax,
            landJiyukCd1 = landJiyukCd1?.split(",")?.filter { it.isNotBlank() },
            landJimokCd = landJimokCd?.split(",")?.filter { it.isNotBlank() },
            landAreaMin = landAreaMin,
            landAreaMax = landAreaMax,
            landPriceMin = landPriceMin,
            landPriceMax = landPriceMax,
            tradeProperty = tradeProperty?.split(",")?.filter { it.isNotBlank() },
            tradeContractDateStart = tradeContractDateStart,
            tradeContractDateEnd = tradeContractDateEnd,
            tradeEffectiveAmountMin = tradeEffectiveAmountMin,
            tradeEffectiveAmountMax = tradeEffectiveAmountMax,
            tradeBuildingAmountPerM2Min = tradeBuildingAmountPerM2Min,
            tradeBuildingAmountPerM2Max = tradeBuildingAmountPerM2Max,
            tradeLandAmountPerM2Min = tradeLandAmountPerM2Min,
            tradeLandAmountPerM2Max = tradeLandAmountPerM2Max
        )
    }
}
