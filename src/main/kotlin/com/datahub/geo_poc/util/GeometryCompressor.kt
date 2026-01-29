package com.datahub.geo_poc.util

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKBWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Geometry 압축 유틸리티
 * JTS Geometry <-> WKB <-> gzip <-> Base64
 */
object GeometryCompressor {

    private val wkbWriter = WKBWriter()
    private val wkbReader = WKBReader()

    /**
     * Geometry -> Base64 (WKB + gzip)
     */
    fun compress(geometry: Geometry?): String? {
        if (geometry == null) return null
        return try {
            val wkb = wkbWriter.write(geometry)
            val gzipped = gzip(wkb)
            Base64.getEncoder().encodeToString(gzipped)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Base64 -> Geometry (gzip 해제 + WKB 파싱)
     */
    fun decompress(base64: String?): Geometry? {
        if (base64.isNullOrBlank()) return null
        return try {
            val gzipped = Base64.getDecoder().decode(base64)
            val wkb = ungzip(gzipped)
            wkbReader.read(wkb)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Geometry -> GeoJSON Map
     */
    fun toGeoJson(geometry: Geometry?): Map<String, Any>? {
        if (geometry == null) return null
        return try {
            GeoJsonUtils.toGeoJson(geometry)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Base64 -> GeoJSON Map (디코딩 + 변환)
     */
    fun decompressToGeoJson(base64: String?): Map<String, Any>? {
        val geometry = decompress(base64) ?: return null
        return toGeoJson(geometry)
    }

    private fun gzip(data: ByteArray): ByteArray {
        ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzos ->
                gzos.write(data)
            }
            return baos.toByteArray()
        }
    }

    private fun ungzip(data: ByteArray): ByteArray {
        ByteArrayInputStream(data).use { bais ->
            GZIPInputStream(bais).use { gzis ->
                return gzis.readBytes()
            }
        }
    }
}