package com.sanghoon.jvm_jst.es.document

import java.math.BigDecimal
import java.time.LocalDate

/**
 * LC Geo (Land Compact Geo) ES 문서
 * geometry를 object로 저장하는 버전 (이스케이프 오버헤드 제거)
 */
data class LandCompactGeoDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: LandDataGeo,
    val building: BuildingData?,
    val lastRealEstateTrade: RealEstateTradeData?
) {
    companion object {
        const val INDEX_NAME = "land_compact_geo"
    }
}

/**
 * 토지 데이터 (geometry를 object로)
 */
data class LandDataGeo(
    val jiyukCd1: String?,
    val jimokCd: String?,
    val area: Double?,
    val price: Long?,
    val center: Map<String, Double>,
    val geometry: Map<String, Any>? = null  // GeoJSON object (렌더링용)
)
