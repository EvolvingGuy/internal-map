package com.datahub.geo_poc.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 마커 중심점
 */
data class MarkerCenter(
    val lat: Double,
    val lon: Double
)

/**
 * 마커 토지 정보
 */
data class MarkerLand(
    val jiyukCd1: String?,
    val jimokCd: String?,
    val area: Double?,
    val price: Long?,
    val geometry: Map<String, Any>?  // GeoJSON Object (geo_shape)
)

/**
 * 마커 건물 정보
 */
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

/**
 * 마커 실거래 정보
 */
data class MarkerTrade(
    val property: String?,
    val contractDate: LocalDate?,
    val effectiveAmount: Long?,
    val buildingAmountPerM2: BigDecimal?,
    val landAmountPerM2: BigDecimal?
)

/**
 * 등기 집계 데이터
 */
data class RegistrationAgg(
    val count: Int,
    val lastAt: LocalDateTime?,
    val myCount: Int,
    val myLastAt: LocalDateTime?
)
