package com.datahub.geo_poc.es.lbt1x16nofm

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LBT_1x16_NOFM (Land Building Trade, 단일 인덱스 16샤드, No Forcemerge) ES 문서
 */
data class Lbt1x16nofmDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val trades: List<RealEstateTradeData>
) {
    companion object {
        const val INDEX_NAME = "lbt_1x16_nofm"
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
