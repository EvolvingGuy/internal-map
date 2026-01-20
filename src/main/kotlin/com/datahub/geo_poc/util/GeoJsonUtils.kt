package com.datahub.geo_poc.util

import org.locationtech.jts.geom.*

/**
 * Geometry → GeoJSON 변환 유틸리티
 */
object GeoJsonUtils {

    fun toGeoJson(geometry: Geometry?): Map<String, Any>? {
        if (geometry == null) return null
        return try {
            when (geometry) {
                is Point -> mapOf(
                    "type" to "Point",
                    "coordinates" to listOf(geometry.x, geometry.y)
                )
                is LineString -> mapOf(
                    "type" to "LineString",
                    "coordinates" to geometry.coordinates.map { listOf(it.x, it.y) }
                )
                is Polygon -> mapOf(
                    "type" to "Polygon",
                    "coordinates" to buildPolygonCoordinates(geometry)
                )
                is MultiPoint -> mapOf(
                    "type" to "MultiPoint",
                    "coordinates" to (0 until geometry.numGeometries).map { i ->
                        val pt = geometry.getGeometryN(i) as Point
                        listOf(pt.x, pt.y)
                    }
                )
                is MultiLineString -> mapOf(
                    "type" to "MultiLineString",
                    "coordinates" to (0 until geometry.numGeometries).map { i ->
                        val ls = geometry.getGeometryN(i) as LineString
                        ls.coordinates.map { listOf(it.x, it.y) }
                    }
                )
                is MultiPolygon -> mapOf(
                    "type" to "MultiPolygon",
                    "coordinates" to (0 until geometry.numGeometries).map { i ->
                        buildPolygonCoordinates(geometry.getGeometryN(i) as Polygon)
                    }
                )
                is GeometryCollection -> mapOf(
                    "type" to "GeometryCollection",
                    "geometries" to (0 until geometry.numGeometries).mapNotNull { i ->
                        toGeoJson(geometry.getGeometryN(i))
                    }
                )
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildPolygonCoordinates(polygon: Polygon): List<List<List<Double>>> {
        val rings = mutableListOf<List<List<Double>>>()
        rings.add(polygon.exteriorRing.coordinates.map { listOf(it.x, it.y) })
        for (i in 0 until polygon.numInteriorRing) {
            rings.add(polygon.getInteriorRingN(i).coordinates.map { listOf(it.x, it.y) })
        }
        return rings
    }
}
