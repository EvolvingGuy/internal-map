package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.dto.LcAggFilter
import com.sanghoon.jvm_jst.es.dto.MarkerRequest
import com.sanghoon.jvm_jst.es.dto.MarkerResponse
import com.sanghoon.jvm_jst.es.service.MarkerService
import org.springframework.beans.factory.annotation.Value
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Marker API Controller
 */
@RestController
@RequestMapping("/api/es/markers")
class MarkerApiController(
    private val markerService: MarkerService
) {
    /**
     * Type 1: Land 우선 조회
     * GET /api/markers/type1
     */
    @GetMapping("/type1")
    fun getMarkersType1(
        // bbox
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        // registration 필터
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) minCreatedDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) maxCreatedDate: LocalDate?,
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
    ): ResponseEntity<MarkerResponse> {
        val request = buildMarkerRequest(
            swLng, swLat, neLng, neLat,
            userId, minCreatedDate, maxCreatedDate,
            buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingPmsDayRecent5y, buildingStcnsDayRecent5y,
            buildingUseAprDayStart, buildingUseAprDayEnd, buildingTotAreaMin, buildingTotAreaMax,
            buildingPlatAreaMin, buildingPlatAreaMax, buildingArchAreaMin, buildingArchAreaMax,
            landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax,
            tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax,
            tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max, tradeLandAmountPerM2Min, tradeLandAmountPerM2Max
        )
        return ResponseEntity.ok(markerService.getMarkersType1(request))
    }

    /**
     * Type 2: Registration 우선 조회
     * GET /api/markers/type2
     */
    @GetMapping("/type2")
    fun getMarkersType2(
        // bbox
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        // registration 필터
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) minCreatedDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) maxCreatedDate: LocalDate?,
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
    ): ResponseEntity<MarkerResponse> {
        val request = buildMarkerRequest(
            swLng, swLat, neLng, neLat,
            userId, minCreatedDate, maxCreatedDate,
            buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingPmsDayRecent5y, buildingStcnsDayRecent5y,
            buildingUseAprDayStart, buildingUseAprDayEnd, buildingTotAreaMin, buildingTotAreaMax,
            buildingPlatAreaMin, buildingPlatAreaMax, buildingArchAreaMin, buildingArchAreaMax,
            landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax,
            tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax,
            tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max, tradeLandAmountPerM2Min, tradeLandAmountPerM2Max
        )
        return ResponseEntity.ok(markerService.getMarkersType2(request))
    }

    private fun buildMarkerRequest(
        swLng: Double, swLat: Double, neLng: Double, neLat: Double,
        userId: Long?, minCreatedDate: LocalDate?, maxCreatedDate: LocalDate?,
        buildingMainPurpsCdNm: String?, buildingRegstrGbCdNm: String?,
        buildingPmsDayRecent5y: Boolean?, buildingStcnsDayRecent5y: Boolean?,
        buildingUseAprDayStart: Int?, buildingUseAprDayEnd: Int?,
        buildingTotAreaMin: BigDecimal?, buildingTotAreaMax: BigDecimal?,
        buildingPlatAreaMin: BigDecimal?, buildingPlatAreaMax: BigDecimal?,
        buildingArchAreaMin: BigDecimal?, buildingArchAreaMax: BigDecimal?,
        landJiyukCd1: String?, landJimokCd: String?,
        landAreaMin: Double?, landAreaMax: Double?,
        landPriceMin: Long?, landPriceMax: Long?,
        tradeProperty: String?,
        tradeContractDateStart: LocalDate?, tradeContractDateEnd: LocalDate?,
        tradeEffectiveAmountMin: Long?, tradeEffectiveAmountMax: Long?,
        tradeBuildingAmountPerM2Min: BigDecimal?, tradeBuildingAmountPerM2Max: BigDecimal?,
        tradeLandAmountPerM2Min: BigDecimal?, tradeLandAmountPerM2Max: BigDecimal?
    ): MarkerRequest {
        val lcFilter = LcAggFilter(
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

        return MarkerRequest(
            swLng = swLng,
            swLat = swLat,
            neLng = neLng,
            neLat = neLat,
            userId = userId,
            minCreatedDate = minCreatedDate,
            maxCreatedDate = maxCreatedDate,
            lcFilter = lcFilter
        )
    }
}

/**
 * Marker Page Controller
 */
@Controller
@RequestMapping("/page/es/markers")
class MarkerPageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping
    fun page(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "Markers - Land + Registration")
        return "es/markers"
    }
}
