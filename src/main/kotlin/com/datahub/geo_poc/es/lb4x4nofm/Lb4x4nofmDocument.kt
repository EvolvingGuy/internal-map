package com.datahub.geo_poc.es.lb4x4nofm

import com.datahub.geo_poc.es.document.common.BuildingData
import java.math.BigDecimal
import java.time.LocalDate

/**
 * LB_4x4_NOFM (Land Building, 4인덱스 x 4샤드, PNU hash % 4, No Forcemerge) ES 문서
 * - buildings: nested (기존과 동일)
 * - trade: flat top1 (가장 최근 실거래 1건만, nested 아님)
 */
data class Lb4x4nofmDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val trade: Trade?  // top1 only, flat
) {
    companion object {
        const val INDEX_PREFIX = "lb_4x4_nofm"
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

    /**
     * 실거래 top1 (flat object, not nested)
     */
    data class Trade(
        val property: String,
        val contractDate: LocalDate,
        val effectiveAmount: Long,
        val buildingAmountPerM2: BigDecimal?,
        val landAmountPerM2: BigDecimal?
    )
}
