package com.sanghoon.jvm_jst.es.document

import java.math.BigDecimal
import java.time.LocalDate

/**
 * LC (Land Compact) ES 문서
 * 필지 단위 인덱스 - 비즈니스 필터 있는 경우 사용
 * SD, SGG, EMD 단위로 terms aggregation
 */
data class LandCompactDocument(
    val pnu: String,          // PNU 코드 19자리
    val sd: String,           // 시도 코드 2자리 (keyword, eagerGlobalOrdinals)
    val sgg: String,          // 시군구 코드 5자리 (keyword, eagerGlobalOrdinals)
    val emd: String,          // 읍면동 코드 10자리 (keyword, eagerGlobalOrdinals)
    val land: LandData,
    val building: BuildingData?,
    val lastRealEstateTrade: RealEstateTradeData?
) {
    companion object {
        const val INDEX_NAME = "land_compact"
    }
}

/**
 * 토지 데이터
 */
data class LandData(
    val jiyukCd1: String?,    // 용도지역 코드
    val jimokCd: String?,     // 지목 코드
    val area: Double?,        // 토지면적
    val price: Long?,         // 개별공시지가
    val center: Map<String, Double>  // { "lat": ..., "lon": ... }
)

/**
 * 건축물 데이터
 */
data class BuildingData(
    val mgmBldrgstPk: String,     // 건축물대장키
    val mainPurpsCdNm: String?,   // 주용도
    val regstrGbCdNm: String?,    // 일반|집합|NULL
    val pmsDay: LocalDate?,       // 허가/신고일
    val stcnsDay: LocalDate?,     // 착공일
    val useAprDay: LocalDate?,    // 준공연도
    val totArea: BigDecimal?,     // 연면적
    val platArea: BigDecimal?,    // 대지면적
    val archArea: BigDecimal?     // 건축면적
)

/**
 * 실거래 데이터
 */
data class RealEstateTradeData(
    val property: String,              // 구분
    val contractDate: LocalDate,       // 거래연월
    val effectiveAmount: Long,         // 거래가
    val buildingAmountPerM2: BigDecimal?,  // 건물면적당단가
    val landAmountPerM2: BigDecimal?   // 토지면적당단가
)
