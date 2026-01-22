package com.sanghoon.jvm_jst.es.document

/**
 * LC Intersect ES 문서
 * geometry를 geo_shape로 저장하여 intersects 쿼리 지원
 */
data class LandCompactIntersectDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: LandDataIntersect,
    val building: BuildingData?,
    val lastRealEstateTrade: RealEstateTradeData?
) {
    companion object {
        const val INDEX_NAME = "land_compact_intersect"
    }
}

/**
 * 토지 데이터 (geometry를 geo_shape로)
 */
data class LandDataIntersect(
    val jiyukCd1: String?,
    val jimokCd: String?,
    val area: Double?,
    val price: Long?,
    val center: Map<String, Double>,
    val geometry: Map<String, Any>?  // GeoJSON for geo_shape query
)
