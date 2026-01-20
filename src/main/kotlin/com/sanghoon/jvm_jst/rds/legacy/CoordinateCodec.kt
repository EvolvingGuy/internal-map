package com.sanghoon.jvm_jst.rds.legacy

/**
 * 좌표 압축 유틸
 * - 위도/경도를 Long 하나에 pack (8 bytes로 두 좌표 저장)
 * - 소수점 6자리 정밀도 (약 10cm) 유지
 * - 위도: -90 ~ 90 → 0 ~ 180,000,000 (28bit면 충분)
 * - 경도: -180 ~ 180 → 0 ~ 360,000,000 (29bit면 충분)
 */
object CoordinateCodec {

    private const val SCALE = 1_000_000  // 소수점 6자리
    private const val LAT_OFFSET = 90.0
    private const val LNG_OFFSET = 180.0

    /**
     * 위도/경도를 Long 하나로 인코딩
     */
    fun encode(lat: Double, lng: Double): Long {
        val latInt = ((lat + LAT_OFFSET) * SCALE).toLong()
        val lngInt = ((lng + LNG_OFFSET) * SCALE).toLong()
        return (latInt shl 32) or (lngInt and 0xFFFFFFFFL)
    }

    /**
     * Long을 위도/경도로 디코딩
     */
    fun decode(packed: Long): Pair<Double, Double> {
        val latInt = (packed shr 32).toInt()
        val lngInt = (packed and 0xFFFFFFFFL).toInt()
        val lat = (latInt.toDouble() / SCALE) - LAT_OFFSET
        val lng = (lngInt.toDouble() / SCALE) - LNG_OFFSET
        return lat to lng
    }

    /**
     * 위도만 추출 (디코딩 없이)
     */
    fun decodeLat(packed: Long): Double {
        val latInt = (packed shr 32).toInt()
        return (latInt.toDouble() / SCALE) - LAT_OFFSET
    }

    /**
     * 경도만 추출 (디코딩 없이)
     */
    fun decodeLng(packed: Long): Double {
        val lngInt = (packed and 0xFFFFFFFFL).toInt()
        return (lngInt.toDouble() / SCALE) - LNG_OFFSET
    }
}
