package com.sanghoon.jvm_jst.rds.common

/**
 * 바운딩 박스 (클라이언트 뷰포트)
 */
data class BBox(
    val swLng: Double,
    val swLat: Double,
    val neLng: Double,
    val neLat: Double
)

/**
 * 행정구역 레벨
 */
enum class RegionLevel(
    val code: String,
    val h3Resolution: Int,
    val staticResolution: Int
) {
    EMD("EMD", 9, 9),
    SGG("SGG", 7, 7),
    SD("SD", 5, 5);

    companion object {
        fun fromCode(code: String): RegionLevel =
            entries.find { it.code == code.uppercase() }
                ?: throw IllegalArgumentException("Unknown region level: $code")
    }
}

/**
 * 공통 요청 DTO
 */
data class AggRequest(
    val swLng: Double,
    val swLat: Double,
    val neLng: Double,
    val neLat: Double
) {
    fun toBBox() = BBox(swLng, swLat, neLng, neLat)
}
