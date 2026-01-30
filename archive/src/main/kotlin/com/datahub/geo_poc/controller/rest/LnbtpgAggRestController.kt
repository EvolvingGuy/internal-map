package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbtpg.LnbtpgAggregationService
import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LcAggFilter
import com.datahub.geo_poc.model.LcAggResponse
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * LNBTPG Aggregation REST Controller
 */
@RestController
@RequestMapping("/api/es/lnbtpg/agg")
class LnbtpgAggRestController(
    private val aggService: LnbtpgAggregationService
) {
    @GetMapping("/sd")
    fun aggregateBySd(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        // Building filters
        @RequestParam(required = false) buildingMainPurpsCdNm: List<String>?,
        @RequestParam(required = false) buildingRegstrGbCdNm: List<String>?,
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
        @RequestParam(required = false) landJiyukCd1: List<String>?,
        @RequestParam(required = false) landJimokCd: List<String>?,
        @RequestParam(required = false) landAreaMin: Double?,
        @RequestParam(required = false) landAreaMax: Double?,
        @RequestParam(required = false) landPriceMin: Long?,
        @RequestParam(required = false) landPriceMax: Long?,
        // Trade filters
        @RequestParam(required = false) tradeProperty: List<String>?,
        @RequestParam(required = false) tradeContractDateStart: LocalDate?,
        @RequestParam(required = false) tradeContractDateEnd: LocalDate?,
        @RequestParam(required = false) tradeEffectiveAmountMin: Long?,
        @RequestParam(required = false) tradeEffectiveAmountMax: Long?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Max: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Max: BigDecimal?
    ): LcAggResponse {
        val bbox = BBoxRequest(swLng, swLat, neLng, neLat)
        val filter = LcAggFilter(
            buildingMainPurpsCdNm = buildingMainPurpsCdNm,
            buildingRegstrGbCdNm = buildingRegstrGbCdNm,
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
            landJiyukCd1 = landJiyukCd1,
            landJimokCd = landJimokCd,
            landAreaMin = landAreaMin,
            landAreaMax = landAreaMax,
            landPriceMin = landPriceMin,
            landPriceMax = landPriceMax,
            tradeProperty = tradeProperty,
            tradeContractDateStart = tradeContractDateStart,
            tradeContractDateEnd = tradeContractDateEnd,
            tradeEffectiveAmountMin = tradeEffectiveAmountMin,
            tradeEffectiveAmountMax = tradeEffectiveAmountMax,
            tradeBuildingAmountPerM2Min = tradeBuildingAmountPerM2Min,
            tradeBuildingAmountPerM2Max = tradeBuildingAmountPerM2Max,
            tradeLandAmountPerM2Min = tradeLandAmountPerM2Min,
            tradeLandAmountPerM2Max = tradeLandAmountPerM2Max
        )
        return aggService.aggregateBySd(bbox, filter)
    }

    @GetMapping("/sgg")
    fun aggregateBySgg(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        @RequestParam(required = false) buildingMainPurpsCdNm: List<String>?,
        @RequestParam(required = false) buildingRegstrGbCdNm: List<String>?,
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
        @RequestParam(required = false) landJiyukCd1: List<String>?,
        @RequestParam(required = false) landJimokCd: List<String>?,
        @RequestParam(required = false) landAreaMin: Double?,
        @RequestParam(required = false) landAreaMax: Double?,
        @RequestParam(required = false) landPriceMin: Long?,
        @RequestParam(required = false) landPriceMax: Long?,
        @RequestParam(required = false) tradeProperty: List<String>?,
        @RequestParam(required = false) tradeContractDateStart: LocalDate?,
        @RequestParam(required = false) tradeContractDateEnd: LocalDate?,
        @RequestParam(required = false) tradeEffectiveAmountMin: Long?,
        @RequestParam(required = false) tradeEffectiveAmountMax: Long?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Max: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Max: BigDecimal?
    ): LcAggResponse {
        val bbox = BBoxRequest(swLng, swLat, neLng, neLat)
        val filter = LcAggFilter(
            buildingMainPurpsCdNm = buildingMainPurpsCdNm,
            buildingRegstrGbCdNm = buildingRegstrGbCdNm,
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
            landJiyukCd1 = landJiyukCd1,
            landJimokCd = landJimokCd,
            landAreaMin = landAreaMin,
            landAreaMax = landAreaMax,
            landPriceMin = landPriceMin,
            landPriceMax = landPriceMax,
            tradeProperty = tradeProperty,
            tradeContractDateStart = tradeContractDateStart,
            tradeContractDateEnd = tradeContractDateEnd,
            tradeEffectiveAmountMin = tradeEffectiveAmountMin,
            tradeEffectiveAmountMax = tradeEffectiveAmountMax,
            tradeBuildingAmountPerM2Min = tradeBuildingAmountPerM2Min,
            tradeBuildingAmountPerM2Max = tradeBuildingAmountPerM2Max,
            tradeLandAmountPerM2Min = tradeLandAmountPerM2Min,
            tradeLandAmountPerM2Max = tradeLandAmountPerM2Max
        )
        return aggService.aggregateBySgg(bbox, filter)
    }

    @GetMapping("/emd")
    fun aggregateByEmd(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        @RequestParam(required = false) buildingMainPurpsCdNm: List<String>?,
        @RequestParam(required = false) buildingRegstrGbCdNm: List<String>?,
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
        @RequestParam(required = false) landJiyukCd1: List<String>?,
        @RequestParam(required = false) landJimokCd: List<String>?,
        @RequestParam(required = false) landAreaMin: Double?,
        @RequestParam(required = false) landAreaMax: Double?,
        @RequestParam(required = false) landPriceMin: Long?,
        @RequestParam(required = false) landPriceMax: Long?,
        @RequestParam(required = false) tradeProperty: List<String>?,
        @RequestParam(required = false) tradeContractDateStart: LocalDate?,
        @RequestParam(required = false) tradeContractDateEnd: LocalDate?,
        @RequestParam(required = false) tradeEffectiveAmountMin: Long?,
        @RequestParam(required = false) tradeEffectiveAmountMax: Long?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeBuildingAmountPerM2Max: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Min: BigDecimal?,
        @RequestParam(required = false) tradeLandAmountPerM2Max: BigDecimal?
    ): LcAggResponse {
        val bbox = BBoxRequest(swLng, swLat, neLng, neLat)
        val filter = LcAggFilter(
            buildingMainPurpsCdNm = buildingMainPurpsCdNm,
            buildingRegstrGbCdNm = buildingRegstrGbCdNm,
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
            landJiyukCd1 = landJiyukCd1,
            landJimokCd = landJimokCd,
            landAreaMin = landAreaMin,
            landAreaMax = landAreaMax,
            landPriceMin = landPriceMin,
            landPriceMax = landPriceMax,
            tradeProperty = tradeProperty,
            tradeContractDateStart = tradeContractDateStart,
            tradeContractDateEnd = tradeContractDateEnd,
            tradeEffectiveAmountMin = tradeEffectiveAmountMin,
            tradeEffectiveAmountMax = tradeEffectiveAmountMax,
            tradeBuildingAmountPerM2Min = tradeBuildingAmountPerM2Min,
            tradeBuildingAmountPerM2Max = tradeBuildingAmountPerM2Max,
            tradeLandAmountPerM2Min = tradeLandAmountPerM2Min,
            tradeLandAmountPerM2Max = tradeLandAmountPerM2Max
        )
        return aggService.aggregateByEmd(bbox, filter)
    }
}
