package com.datahub.geo_poc.es.document.land

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LNBTPS (Land Nested Building Trade Point SD) ES 문서
 * LNBTP와 동일한 구조, 시도(SD)별 개별 인덱스로 분할 (17개)
 */
data class LnbtpsDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val buildings: List<BuildingData>,
    val trades: List<RealEstateTradeData>
) {
    companion object {
        const val INDEX_PREFIX = "lnbtps"

        // 시도 코드 목록 (17개)
        val SD_CODES = listOf(
            "11", "26", "27", "28", "29", "30", "31", "36",  // 특별/광역시, 세종
            "41", "43", "44", "46", "47", "48", "50", "51", "52"  // 도, 제주, 강원, 전북
        )

        fun indexName(sd: String): String = "${INDEX_PREFIX}_$sd"

        fun allIndexNames(): List<String> = SD_CODES.map { indexName(it) }

        fun indexPattern(): String = "${INDEX_PREFIX}_*"
    }

    data class Land(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?,
        val center: Map<String, Double>
    )
}
