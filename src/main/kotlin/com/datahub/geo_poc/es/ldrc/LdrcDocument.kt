package com.datahub.geo_poc.es.ldrc

import com.datahub.geo_poc.jpa.entity.PnuAggEmd10
import com.uber.h3core.H3Core

/**
 * LDRC (Land Dynamic Region Cluster) ES 문서
 * 동적 행정구역 클러스터 - H3 기반 뷰포트 집계
 */
data class LdrcDocument(
    val id: String = "",
    val level: String = "",
    val h3Index: Long = 0,
    val regionCode: Long = 0,
    val cnt: Int = 0,
    val sumLat: Double = 0.0,
    val sumLng: Double = 0.0,
    val sgg7: Long? = null,
    val sd5: Long? = null,
    val regionCodeSgg: Long? = null,
    val regionCodeSd: Long? = null
) {
    companion object {
        const val INDEX_NAME = "land_dynamic_region_cluster"

        fun fromEmd10(entity: PnuAggEmd10, h3: H3Core): LdrcDocument {
            val codeStr = entity.code.toString()
            return LdrcDocument(
                id = "EMD_${entity.h3Index}_${entity.code}",
                level = "EMD",
                h3Index = entity.h3Index,
                regionCode = entity.code,
                cnt = entity.cnt,
                sumLat = entity.sumLat,
                sumLng = entity.sumLng,
                sgg7 = h3.cellToParent(entity.h3Index, 7),
                sd5 = h3.cellToParent(entity.h3Index, 5),
                regionCodeSgg = codeStr.take(5).toLongOrNull(),
                regionCodeSd = codeStr.take(2).toLongOrNull()
            )
        }
    }
}
