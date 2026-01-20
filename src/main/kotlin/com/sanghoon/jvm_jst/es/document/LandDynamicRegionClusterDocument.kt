package com.sanghoon.jvm_jst.es.document

import com.sanghoon.jvm_jst.rds.entity.PnuAggEmd10

/**
 * LDRC (Land Dynamic Region Cluster) ES 문서
 * 가변형 행정구역 클러스터 - 비즈니스 필터 없는 경우 사용
 * H3 기반으로 좌표 이동에 따라 행정구역별 데이터가 변동됨
 */
data class LandDynamicRegionClusterDocument(
    val id: String = "",              // "{level}{code}{h3Index}"
    val level: String = "",           // SD, SGG, EMD
    val code: Long = 0,               // 행정구역 코드
    val h3Index: Long = 0,            // H3 셀 인덱스
    val count: Int = 0,               // 필지 수
    val sumLat: Double = 0.0,         // 위도 합계
    val sumLng: Double = 0.0          // 경도 합계
) {
    companion object {
        const val INDEX_NAME = "land_dynamic_region_cluster"

        fun fromEmd10(entity: PnuAggEmd10): LandDynamicRegionClusterDocument {
            return LandDynamicRegionClusterDocument(
                id = "EMD${entity.code}${entity.h3Index}",
                level = "EMD",
                code = entity.code,
                h3Index = entity.h3Index,
                count = entity.cnt,
                sumLat = entity.sumLat,
                sumLng = entity.sumLng
            )
        }
    }
}

/**
 * LDRC 집계 데이터 (SGG/SD 인덱싱용)
 */
data class LdrcAggData(
    val h3Index: Long,
    val code: Long,
    var count: Long = 0,
    var sumLat: Double = 0.0,
    var sumLng: Double = 0.0
) {
    fun add(cnt: Int, srcSumLat: Double, srcSumLng: Double) {
        count += cnt
        sumLat += srcSumLat
        sumLng += srcSumLng
    }

    fun toDocument(level: String): LandDynamicRegionClusterDocument {
        return LandDynamicRegionClusterDocument(
            id = "${level}${code}${h3Index}",
            level = level,
            code = code,
            h3Index = h3Index,
            count = count.toInt(),
            sumLat = sumLat,
            sumLng = sumLng
        )
    }
}
