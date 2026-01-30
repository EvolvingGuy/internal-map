package com.datahub.geo_poc.es.document.land

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LNBTP16 (Land Nested Building Trade Point 16-Shard) ES 문서
 * LNBTP와 동일한 구조, 단일 인덱스 16샤드
 */
data class Lnbtp16Document(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val trades: List<RealEstateTradeData>
) {
    companion object {
        const val INDEX_NAME = "land_nested_building_trade_point_16s"
        const val SHARD_COUNT = 16
    }

    data class Land(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?,
        val center: Map<String, Double>
    )
}
