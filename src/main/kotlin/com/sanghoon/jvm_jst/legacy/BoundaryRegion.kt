package com.sanghoon.jvm_jst.legacy

import org.locationtech.jts.geom.Geometry

data class BoundaryRegion(
    val regionCode: String,
    val regionKoreanName: String,
    val geom: Geometry,
    val centerLng: Double,
    val centerLat: Double,
    val gubun: String
)
