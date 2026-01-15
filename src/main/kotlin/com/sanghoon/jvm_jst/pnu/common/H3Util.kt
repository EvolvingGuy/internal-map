package com.sanghoon.jvm_jst.pnu.common

import com.uber.h3core.H3Core
import com.uber.h3core.util.LatLng

/**
 * H3 유틸리티
 */
object H3Util {
    private val h3: H3Core = H3Core.newInstance()

    /**
     * bbox → H3 인덱스 목록 (Long)
     */
    fun bboxToH3Indexes(bbox: BBox, resolution: Int): List<Long> {
        val polygon = listOf(
            LatLng(bbox.swLat, bbox.swLng), // SW
            LatLng(bbox.neLat, bbox.swLng), // NW
            LatLng(bbox.neLat, bbox.neLng), // NE
            LatLng(bbox.swLat, bbox.neLng)  // SE
        )
        return h3.polygonToCells(polygon, emptyList(), resolution)
    }

    /**
     * H3 Long → String 변환
     */
    fun h3ToString(h3Index: Long): String = h3.h3ToString(h3Index)

    /**
     * H3 String → Long 변환
     */
    fun stringToH3(h3Index: String): Long = h3.stringToH3(h3Index)
}
