package com.datahub.geo_poc.es.document.land

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LC (Land Compact) ES 문서
 * 필지 단위 인덱스 - geometry를 geo_shape로 저장하여 intersects 쿼리 지원
 */
data class LandCompactDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val building: BuildingData?,
    val lastRealEstateTrade: RealEstateTradeData?
) {
    companion object {
        const val INDEX_NAME = "land_compact"
    }

    data class Land(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?,
        val center: Map<String, Double>,
        val geometry: Map<String, Any>?
    )
}
