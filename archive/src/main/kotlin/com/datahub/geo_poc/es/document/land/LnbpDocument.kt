package com.datahub.geo_poc.es.document.land

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LNBP (Land Nested Building Point) ES 문서
 * 필지 단위 인덱스 - 건물을 nested array로 저장하며, geo_point만 사용 (geo_shape 없음)
 */
data class LnbpDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val lastRealEstateTrade: RealEstateTradeData?
) {
    companion object {
        const val INDEX_NAME = "land_nested_building_point"
    }

    data class Land(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?,
        val center: Map<String, Double>
        // NO geometry field - key difference from LNB
    )
}
