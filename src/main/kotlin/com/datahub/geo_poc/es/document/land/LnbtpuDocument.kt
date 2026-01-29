package com.datahub.geo_poc.es.document.land

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData
import kotlin.math.absoluteValue

/**
 * LNBTPU (Land Nested Building Trade Point Uniform) ES 문서
 * LNBTP와 동일한 구조, 4개 파티션 인덱스로 균등 분할 (PNU hash % 4)
 */
data class LnbtpuDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val trades: List<RealEstateTradeData>
) {
    companion object {
        const val INDEX_PREFIX = "lnbtpu"
        const val PARTITION_COUNT = 4

        fun indexName(partition: Int): String = "${INDEX_PREFIX}_$partition"

        fun allIndexNames(): List<String> = (1..PARTITION_COUNT).map { indexName(it) }

        /**
         * PNU 코드 기반 파티션 결정 (hash % 4 + 1) - 균등 분산
         */
        fun getPartition(pnu: String): Int = (pnu.hashCode().absoluteValue % PARTITION_COUNT) + 1
    }

    data class Land(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?,
        val center: Map<String, Double>
    )
}
