package com.datahub.geo_poc.es.document.land

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LNBTPG (Land Nested Building Trade Point Geometry) ES 문서
 * LNBTP + geometry 바이너리 저장 - 마커 API 전용
 */
data class LnbtpgDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val geometryBinary: String?,  // WKB + gzip + Base64
    val buildings: List<BuildingData>,
    val trades: List<RealEstateTradeData>
) {
    companion object {
        const val INDEX_NAME = "land_nested_building_trade_point_geo"
    }

    data class Land(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?,
        val center: Map<String, Double>
    )
}
