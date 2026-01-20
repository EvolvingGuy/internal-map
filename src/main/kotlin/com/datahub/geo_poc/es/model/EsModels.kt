package com.datahub.geo_poc.es.model

import com.datahub.geo_poc.model.*

/**
 * Land Compact 조회 결과 데이터
 */
data class LandCompactData(
    val pnu: String,
    val center: MarkerCenter,
    val land: MarkerLand?,
    val building: MarkerBuilding?,
    val trade: MarkerTrade?
)

/**
 * LDRC 조회 응답
 */
data class LdrcResponse(
    val level: String,
    val h3Count: Int,
    val clusters: List<LdrcCluster>,
    val totalCount: Long,
    val elapsedMs: Long
)

/**
 * LDRC 클러스터
 */
data class LdrcCluster(
    val code: Long,
    val name: String,
    val count: Long,
    val centerLat: Double,
    val centerLng: Double
)

/**
 * LSRC 행정구역 데이터
 */
data class LsrcRegionData(
    val code: String,
    val name: String,
    val cnt: Int,
    val centerLat: Double,
    val centerLng: Double
)

/**
 * LSRC 조회 결과
 */
data class LsrcQueryResponse(
    val regions: List<LsrcRegionData>,
    val totalCount: Int,
    val elapsedMs: Long
)
