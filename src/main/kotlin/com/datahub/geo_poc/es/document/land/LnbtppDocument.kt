package com.datahub.geo_poc.es.document.land

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData
import kotlin.math.absoluteValue

/**
 * LNBTPP (Land Nested Building Trade Point Partitioned) ES 문서
 * LNBTP와 동일한 구조, 4개 파티션 인덱스로 분할 (EMD hash % 4)
 */
data class LnbtppDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val trades: List<RealEstateTradeData>
) {
    companion object {
        const val INDEX_PREFIX = "lnbtp"
        const val PARTITION_COUNT = 4

        fun indexName(partition: Int): String = "${INDEX_PREFIX}_$partition"

        fun allIndexNames(): List<String> = (1..PARTITION_COUNT).map { indexName(it) }

        /**
         * EMD 코드 기반 파티션 결정 (hash % 4 + 1) - 비균등 분산
         */
        fun getPartition(emd: String): Int = (emd.hashCode().absoluteValue % PARTITION_COUNT) + 1
    }

    data class Land(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?,
        val center: Map<String, Double>
    )
}
