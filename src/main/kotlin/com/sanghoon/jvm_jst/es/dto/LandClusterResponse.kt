package com.sanghoon.jvm_jst.es.dto

/**
 * 클러스터 아이템
 */
data class ClusterItem(
    val code: Int,
    val name: String,
    val centerLat: Double,
    val centerLng: Double,
    val count: Long
)

/**
 * 토지 클러스터 조회 응답
 */
data class LandClusterResponse(
    val clusterType: ClusterType,
    val totalCount: Long,
    val clusters: List<ClusterItem>,
    val elapsedMs: Long
)
