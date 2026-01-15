package com.sanghoon.jvm_jst.legacy

import com.github.luben.zstd.ZstdInputStream
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.index.strtree.STRtree
import org.locationtech.jts.io.WKTReader
import org.springframework.core.io.ClassPathResource
import java.io.InputStreamReader

enum class RegionLevel {
    SIDO, SIGUNGU, DONG;

    companion object {
        fun from(gubun: String): RegionLevel = when (gubun) {
            "sido" -> SIDO
            "sigungu" -> SIGUNGU
            "dong", "li" -> DONG
            else -> throw IllegalArgumentException("Unknown gubun: $gubun")
        }
    }
}

object BoundaryRegionCache {

    private val geometryFactory = GeometryFactory()
    private val wktReader = WKTReader(geometryFactory)
    private lateinit var regions: List<BoundaryRegion>
    private lateinit var regionsByLevel: Map<RegionLevel, List<BoundaryRegion>>
    private lateinit var treesByLevel: Map<RegionLevel, STRtree>

    fun initialize() {
        val resource = ClassPathResource("geo.csv.zst")
        val loadedRegions = mutableListOf<BoundaryRegion>()

        ZstdInputStream(resource.inputStream).use { zstdStream ->
            InputStreamReader(zstdStream, Charsets.UTF_8).use { reader ->
                CSVParser.parse(reader, CSVFormat.DEFAULT).use { csvParser ->
                    for (record in csvParser) {
                        if (record.size() >= 6) {
                            val region = BoundaryRegion(
                                regionCode = record[0],
                                regionKoreanName = record[1],
                                geom = wktReader.read(record[2]),
                                centerLng = record[3].toDouble(),
                                centerLat = record[4].toDouble(),
                                gubun = record[5].trim()
                            )
                            loadedRegions.add(region)
                        }
                    }
                }
            }
        }

        regions = loadedRegions
        regionsByLevel = regions.groupBy { RegionLevel.from(it.gubun) }

        // STRtree 인덱스 구축
        treesByLevel = RegionLevel.entries.associateWith { level ->
            STRtree().apply {
                regionsByLevel[level]?.forEach { region ->
                    insert(region.geom.envelopeInternal, region)
                }
                build()
            }
        }
    }

    fun findIntersecting(swLng: Double, swLat: Double, neLng: Double, neLat: Double, level: RegionLevel): List<BoundaryRegion> {
        if (!::treesByLevel.isInitialized) return emptyList()

        val tree = treesByLevel[level] ?: return emptyList()
        val bbox = createBoundingBox(swLng, swLat, neLng, neLat)

        @Suppress("UNCHECKED_CAST")
        val candidates = tree.query(bbox.envelopeInternal) as List<BoundaryRegion>

        return candidates.filter { it.geom.intersects(bbox) }
    }

    private fun createBoundingBox(swLng: Double, swLat: Double, neLng: Double, neLat: Double): Polygon {
        val coordinates = arrayOf(
            Coordinate(swLng, swLat),
            Coordinate(neLng, swLat),
            Coordinate(neLng, neLat),
            Coordinate(swLng, neLat),
            Coordinate(swLng, swLat)
        )
        return geometryFactory.createPolygon(coordinates)
    }

    fun getRegionCount(): Int = if (::regions.isInitialized) regions.size else 0

    fun getLevels(): Set<RegionLevel> = if (::regionsByLevel.isInitialized) regionsByLevel.keys else emptySet()

    fun getCountByLevel(): Map<RegionLevel, Int> = if (::regionsByLevel.isInitialized) {
        regionsByLevel.mapValues { it.value.size }
    } else emptyMap()

    /**
     * 특정 시도 코드로 시작하는 DONG 레벨 지역 코드 목록 반환
     */
    fun getDongCodesBySido(sidoCodes: Collection<String>): List<String> {
        if (!::regionsByLevel.isInitialized) return emptyList()
        val dongs = regionsByLevel[RegionLevel.DONG] ?: return emptyList()
        return dongs
            .filter { it.regionCode.take(2) in sidoCodes }
            .map { it.regionCode }
    }

    /**
     * 전체 DONG 레벨 지역 코드 반환
     */
    fun getAllDongCodes(): List<String> {
        if (!::regionsByLevel.isInitialized) return emptyList()
        val dongs = regionsByLevel[RegionLevel.DONG] ?: return emptyList()
        return dongs.map { it.regionCode }
    }
}

