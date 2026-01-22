package com.sanghoon.jvm_jst.es.dto

import com.sanghoon.jvm_jst.es.service.MarkerLandGeo

/**
 * Marker Geo API 응답
 * geometry를 object로 반환
 */
data class MarkerGeoResponse(
    val totalCount: Int,
    val elapsedMs: Long,
    val items: List<MarkerGeoItem>
)

/**
 * Marker Geo 아이템
 * geometry가 Map<String, Any>
 */
data class MarkerGeoItem(
    val pnu: String,
    val center: MarkerCenter,
    val land: MarkerLandGeo?,
    val building: MarkerBuilding?,
    val lastRealEstateTrade: MarkerTrade?,
    val registration: RegistrationAgg?
)
