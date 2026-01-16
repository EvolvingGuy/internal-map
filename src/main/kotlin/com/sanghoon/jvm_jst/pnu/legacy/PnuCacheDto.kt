package com.sanghoon.jvm_jst.pnu.legacy

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Redis 저장용 PNU DTO (최소 키)
 * - p: PNU 코드
 * - c: 압축된 좌표 (Long, CoordinateCodec으로 인코딩)
 */
data class PnuCacheDto(
    @JsonProperty("p") val p: String,    // pnu
    @JsonProperty("c") val c: Long       // compressed coordinate
) {
    fun decodeLat(): Double = CoordinateCodec.decodeLat(c)
    fun decodeLng(): Double = CoordinateCodec.decodeLng(c)
    fun decodeCoordinate(): Pair<Double, Double> = CoordinateCodec.decode(c)
}
