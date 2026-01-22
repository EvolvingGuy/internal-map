package com.datahub.geo_poc.model

/**
 * 바운딩 박스 (클라이언트 뷰포트)
 */
data class BBox(
    val swLng: Double,
    val swLat: Double,
    val neLng: Double,
    val neLat: Double
)
