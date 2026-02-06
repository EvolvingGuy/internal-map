package com.datahub.geo_poc.es.lbt4x4nofmoffri

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LBT_4x4_NOFM_OFFRI (Land Building Trade, 4인덱스 x 4샤드, PNU hash % 4, No Forcemerge, Off Refresh Interval) ES 문서
 * - refresh_interval: -1 (인덱싱 중 비활성)
 * - 인덱싱 완료 후 수동 설정 복구 필요
 */
data class Lbt4x4nofmoffriDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val trades: List<RealEstateTradeData>
) {
    companion object {
        const val INDEX_PREFIX = "lbt_4x4_nofm_offri"
        const val INDEX_COUNT = 4
        const val SHARD_COUNT = 4

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
