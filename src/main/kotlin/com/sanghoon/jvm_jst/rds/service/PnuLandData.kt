package com.sanghoon.jvm_jst.rds.service

import com.fasterxml.jackson.annotation.JsonIgnore
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKBWriter

/**
 * PNU 토지 데이터 (캐시/응답 공용 모델)
 */
data class PnuLandData(
    val pnu: String,
    @JsonIgnore val geometry: Geometry,   // Polygon 또는 MultiPolygon
    @JsonIgnore val center: Point,
    val area: Double?,
    val isDonut: Boolean?
) {
    // JSON 응답용
    val centerLat: Double get() = center.y
    val centerLng: Double get() = center.x
    companion object {
        private val wkbReader = WKBReader()
        private val wkbWriter = WKBWriter()

        /**
         * WKB 바이너리에서 PnuLandData 생성
         */
        fun fromWkb(
            pnu: String,
            geometryWkb: ByteArray,
            centerWkb: ByteArray,
            area: Double?,
            isDonut: Boolean?
        ): PnuLandData {
            val geometry = wkbReader.read(geometryWkb)
            val center = wkbReader.read(centerWkb) as Point
            return PnuLandData(pnu, geometry, center, area, isDonut)
        }
    }

    /**
     * geometry를 WKB 바이너리로 변환
     */
    fun geometryToWkb(): ByteArray = wkbWriter.write(geometry)

    /**
     * center를 WKB 바이너리로 변환
     */
    fun centerToWkb(): ByteArray = wkbWriter.write(center)
}
