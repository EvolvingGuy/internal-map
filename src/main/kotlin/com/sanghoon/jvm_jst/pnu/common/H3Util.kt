package com.sanghoon.jvm_jst.pnu.common

import com.uber.h3core.H3Core
import com.uber.h3core.util.LatLng

/**
 * H3 유틸리티
 */
object H3Util {
    private val h3: H3Core = H3Core.newInstance()

    // 남한 영역 (영해 포함)
    private const val KOREA_SW_LNG = 124.0
    private const val KOREA_SW_LAT = 32.5
    private const val KOREA_NE_LNG = 132.0
    private const val KOREA_NE_LAT = 38.6

    /**
     * bbox → H3 인덱스 목록 (Long)
     * 한국 영역으로 클리핑됨
     */
    fun bboxToH3Indexes(bbox: BBox, resolution: Int): List<Long> {
        val clipped = clipToKorea(bbox) ?: return emptyList()

        val polygon = listOf(
            LatLng(clipped.swLat, clipped.swLng), // SW
            LatLng(clipped.neLat, clipped.swLng), // NW
            LatLng(clipped.neLat, clipped.neLng), // NE
            LatLng(clipped.swLat, clipped.neLng)  // SE
        )
        return h3.polygonToCells(polygon, emptyList(), resolution)
    }

    /**
     * bbox를 한국 영역으로 클리핑
     * 교집합이 없으면 null 반환
     */
    private fun clipToKorea(bbox: BBox): BBox? {
        val swLng = maxOf(bbox.swLng, KOREA_SW_LNG)
        val swLat = maxOf(bbox.swLat, KOREA_SW_LAT)
        val neLng = minOf(bbox.neLng, KOREA_NE_LNG)
        val neLat = minOf(bbox.neLat, KOREA_NE_LAT)

        // 교집합이 없으면 null
        if (swLng >= neLng || swLat >= neLat) return null

        return BBox(swLng, swLat, neLng, neLat)
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
