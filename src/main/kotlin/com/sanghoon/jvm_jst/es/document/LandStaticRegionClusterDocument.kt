package com.sanghoon.jvm_jst.es.document

/**
 * LSRC (Land Static Region Cluster) ES 문서
 * 고정형 행정구역 클러스터 - 비즈니스 필터 없는 경우 사용
 */
data class LandStaticRegionClusterDocument(
    val id: String = "",
    val level: String = "",
    val code: String = "",
    val name: String = "",
    val count: Int = 0,
    val center: Map<String, Double> = emptyMap()
) {
    companion object {
        const val INDEX_NAME = "land_static_region_cluster"
    }
}

/**
 * LSRC 집계 데이터 (인덱싱용)
 */
data class LsrcAggData(
    val code: String,
    var count: Long = 0,
    var sumLat: Double = 0.0,
    var sumLng: Double = 0.0
) {
    fun add(lat: Double, lng: Double) {
        count++
        sumLat += lat
        sumLng += lng
    }

    fun toDocument(level: String, name: String): LandStaticRegionClusterDocument {
        return LandStaticRegionClusterDocument(
            id = "${level}_${code}",
            level = level,
            code = code,
            name = name,
            count = count.toInt(),
            center = if (count > 0) {
                mapOf("lat" to sumLat / count, "lon" to sumLng / count)
            } else {
                mapOf("lat" to 0.0, "lon" to 0.0)
            }
        )
    }
}
