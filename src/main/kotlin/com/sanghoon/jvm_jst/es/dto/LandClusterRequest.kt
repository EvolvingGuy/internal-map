package com.sanghoon.jvm_jst.es.dto

/**
 * 클러스터 타입
 */
enum class ClusterType {
    SD,    // 시도
    SGG,   // 시군구
    EMD,   // 읍면동
    GRID   // 그리드
}

/**
 * 토지 클러스터 조회 요청
 */
data class LandClusterRequest(
    // bbox
    val southwestLatitude: Double,
    val southwestLongitude: Double,
    val northeastLatitude: Double,
    val northeastLongitude: Double,

    // 클러스터 타입
    val clusterType: ClusterType,

    // 필터 조건
    val jimokCd: Set<String> = emptySet(),
    val jiyukCd1: Set<String> = emptySet(),
    val mainPurpsCd: Set<String> = emptySet()
) {
    // bbox 편의 프로퍼티
    val swLat: Double get() = southwestLatitude
    val swLng: Double get() = southwestLongitude
    val neLat: Double get() = northeastLatitude
    val neLng: Double get() = northeastLongitude

    fun hasFilters(): Boolean {
        return jimokCd.isNotEmpty() || jiyukCd1.isNotEmpty() || mainPurpsCd.isNotEmpty()
    }
}
