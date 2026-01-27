package com.datahub.geo_poc.es.document.land

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LNB (Land Nested Building) ES 문서
 * 필지 단위 인덱스 - 건물을 nested array로 저장하여 개별 건물 필터링 지원
 */
data class LnbDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val lastRealEstateTrade: RealEstateTradeData?
) {
    companion object {
        const val INDEX_NAME = "land_nested_building"
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
