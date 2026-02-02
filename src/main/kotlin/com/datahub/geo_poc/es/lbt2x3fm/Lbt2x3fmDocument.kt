package com.datahub.geo_poc.es.lbt2x3fm

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LBT_2x3_FM (Land Building Trade, 2인덱스 x 3샤드, PNU hash % 2, Forcemerge) ES 문서
 */
data class Lbt2x3fmDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val trades: List<RealEstateTradeData>
) {
    companion object {
        const val INDEX_PREFIX = "lbt_2x3_fm"
        const val INDEX_COUNT = 2
        const val SHARD_COUNT = 3

        fun indexName(partition: Int): String = "${INDEX_PREFIX}_${partition}"
        fun allIndexPattern(): String = "${INDEX_PREFIX}_*"
        fun allIndexNames(): List<String> = (1..INDEX_COUNT).map { indexName(it) }
        fun resolvePartition(pnu: String): Int = (Math.abs(pnu.hashCode()) % INDEX_COUNT) + 1
    }

    data class Land(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?,
        val center: Map<String, Double>
    )
}
