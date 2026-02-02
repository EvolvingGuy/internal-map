package com.datahub.geo_poc.es.lbt17x2regionnofm

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LBT_17x2_REGION_NOFM (Land Building Trade, 17인덱스 x 2샤드, SD code 기반, No Forcemerge) ES 문서
 */
data class Lbt17x2regionNofmDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val trades: List<RealEstateTradeData>
) {
    companion object {
        const val INDEX_PREFIX = "lbt_17x2_region_nofm"
        const val INDEX_COUNT = 17
        const val SHARD_COUNT = 2

        val SD_CODES = listOf("11", "26", "27", "28", "29", "30", "31", "36", "41", "43", "44", "46", "47", "48", "50", "51", "52")

        fun indexName(sdCode: String): String = "${INDEX_PREFIX}_${sdCode}"
        fun allIndexPattern(): String = "${INDEX_PREFIX}_*"
        fun allIndexNames(): List<String> = SD_CODES.map { indexName(it) }
        fun resolveIndex(pnu: String): String = indexName(pnu.substring(0, 2))
    }

    data class Land(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?,
        val center: Map<String, Double>
    )
}
