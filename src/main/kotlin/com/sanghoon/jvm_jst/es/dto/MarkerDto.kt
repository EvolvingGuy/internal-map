package com.sanghoon.jvm_jst.es.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Marker API 요청
 */
data class MarkerRequest(
    // bbox
    val swLng: Double,
    val swLat: Double,
    val neLng: Double,
    val neLat: Double,
    // registration 필터
    val userId: Long? = null,
    val minCreatedDate: LocalDate? = null,
    val maxCreatedDate: LocalDate? = null,
    // land_compact 필터 (LcAggFilter)
    val lcFilter: LcAggFilter = LcAggFilter()
)

/**
 * Marker API 응답
 */
data class MarkerResponse(
    val totalCount: Int,
    val elapsedMs: Long,
    val items: List<MarkerItem>
)

/**
 * 개별 마커 아이템
 * land_compact 문서 + registration 집계
 */
data class MarkerItem(
    val pnu: String,
    val center: MarkerCenter,
    val land: MarkerLand?,
    val building: MarkerBuilding?,
    val lastRealEstateTrade: MarkerTrade?,
    val registration: RegistrationAgg?
)

data class MarkerCenter(
    val lat: Double,
    val lon: Double
)

data class MarkerLand(
    val jiyukCd1: String?,
    val jimokCd: String?,
    val area: Double?,
    val price: Long?,
    val geometry: String?  // GeoJSON 문자열 (렌더링용)
)

data class MarkerBuilding(
    val mgmBldrgstPk: String?,
    val mainPurpsCdNm: String?,
    val regstrGbCdNm: String?,
    val pmsDay: LocalDate?,
    val stcnsDay: LocalDate?,
    val useAprDay: LocalDate?,
    val totArea: BigDecimal?,
    val platArea: BigDecimal?,
    val archArea: BigDecimal?
)

data class MarkerTrade(
    val property: String?,
    val contractDate: LocalDate?,
    val effectiveAmount: Long?,
    val buildingAmountPerM2: BigDecimal?,
    val landAmountPerM2: BigDecimal?
)

/**
 * Registration 집계 데이터
 */
data class RegistrationAgg(
    val count: Int,
    val lastAt: LocalDateTime?,
    val myCount: Int,
    val myLastAt: LocalDateTime?
)
