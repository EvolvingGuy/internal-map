package com.datahub.geo_poc.es.l4x4nofm

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * L_4x4_NOFM (Land + 단일 Building/Trade, 4인덱스 x 4샤드, PNU hash % 4, No Forcemerge) ES 문서
 *
 * lbt4x4nofm과 동일한 필드를 가지되, buildings/trades가 nested array가 아닌 단일 object.
 * - building: 최신 사용승인일(useAprDay) 기준 1건
 * - trade: 최신 계약일(contractDate) 기준 1건
 */
data class L4x4nofmDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val building: BuildingData?,
    val trade: RealEstateTradeData?
) {
    companion object {
        const val INDEX_PREFIX = "l_4x4_nofm"
        const val INDEX_COUNT = 4
        const val SHARD_COUNT = 4

        fun indexName(partition: Int): String = "${INDEX_PREFIX}_${partition}"
        fun allIndexPattern(): String = "${INDEX_PREFIX}_*"
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
