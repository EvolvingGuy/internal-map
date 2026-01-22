package com.sanghoon.jvm_jst.es.document

import com.sanghoon.jvm_jst.rds.entity.PnuAggEmd10

/**
 * LDRC (Land Dynamic Region Cluster) ES 인덱스 문서 (무필터용)
 */
data class LdrcDocument(
    val id: String = "",           // "{level}_{h3Index}_{code}"
    val level: String = "",        // SD, SGG, EMD
    val h3Index: Long = 0,         // H3 인덱스
    val regionCode: Long = 0,      // 행정구역 코드
    val cnt: Int = 0,              // 필지 수
    val sumLat: Double = 0.0,      // 위도 합
    val sumLng: Double = 0.0       // 경도 합
) {
    companion object {
        const val INDEX_NAME = "land_dynamic_region_cluster"

        fun fromEmd10(entity: PnuAggEmd10): LdrcDocument {
            return LdrcDocument(
                id = "EMD_${entity.h3Index}_${entity.code}",
                level = "EMD",
                h3Index = entity.h3Index,
                regionCode = entity.code,
                cnt = entity.cnt,
                sumLat = entity.sumLat,
                sumLng = entity.sumLng
            )
        }
    }
}
