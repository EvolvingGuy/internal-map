package com.sanghoon.jvm_jst.es.document

import com.sanghoon.jvm_jst.rds.entity.PnuAggEmd10

/**
 * H3 집계 ES 인덱스 문서 (무필터용)
 */
data class H3AggDocument(
    val id: String,           // "{level}_{h3Index}_{code}"
    val level: String,        // SD, SGG, EMD
    val h3Index: Long,        // H3 인덱스
    val regionCode: Long,     // 행정구역 코드
    val cnt: Int,             // 필지 수
    val sumLat: Double,       // 위도 합
    val sumLng: Double        // 경도 합
) {
    companion object {
        fun fromEmd10(entity: PnuAggEmd10): H3AggDocument {
            return H3AggDocument(
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
