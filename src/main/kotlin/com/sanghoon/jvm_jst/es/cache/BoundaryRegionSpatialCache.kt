package com.sanghoon.jvm_jst.es.cache

import com.github.luben.zstd.ZstdInputStream
import jakarta.annotation.PostConstruct
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.index.strtree.STRtree
import org.locationtech.jts.io.WKTReader
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.InputStreamReader

/**
 * 행정구역 경계 공간 캐시
 * 부팅 시 geo.csv.zst를 로드하여 STRtree 인덱스 구축
 * 현재 미사용 - 필요 시 @Component 활성화
 */
// @Component
class BoundaryRegionSpatialCache {

    private val log = LoggerFactory.getLogger(javaClass)
    private val geometryFactory = GeometryFactory()
    private val wktReader = WKTReader(geometryFactory)

    private lateinit var regions: List<CachedBoundaryRegion>
    private lateinit var regionsByLevel: Map<RegionLevel, List<CachedBoundaryRegion>>
    private lateinit var treesByLevel: Map<RegionLevel, STRtree>

    @PostConstruct
    fun initialize() {
        log.info("[BoundaryRegionSpatialCache] 초기화 시작")
        val startTime = System.currentTimeMillis()

        val resource = ClassPathResource("geo.csv.zst")
        val loadedRegions = mutableListOf<CachedBoundaryRegion>()

        ZstdInputStream(resource.inputStream).use { zstdStream ->
            InputStreamReader(zstdStream, Charsets.UTF_8).use { reader ->
                CSVParser.parse(reader, CSVFormat.DEFAULT).use { csvParser ->
                    for (record in csvParser) {
                        if (record.size() >= 6) {
                            val region = CachedBoundaryRegion(
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

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[BoundaryRegionSpatialCache] 초기화 완료 - 총 {}개 지역, 소요시간: {}ms", regions.size, elapsed)
        regionsByLevel.forEach { (level, list) ->
            log.info("[BoundaryRegionSpatialCache] {} 레벨: {}개", level, list.size)
        }
    }

    /**
     * bbox와 교차하는 지역 목록 반환
     */
    fun findIntersecting(swLng: Double, swLat: Double, neLng: Double, neLat: Double, level: RegionLevel): List<CachedBoundaryRegion> {
        if (!::treesByLevel.isInitialized) return emptyList()

        val tree = treesByLevel[level] ?: return emptyList()
        val bbox = createBoundingBox(swLng, swLat, neLng, neLat)

        @Suppress("UNCHECKED_CAST")
        val candidates = tree.query(bbox.envelopeInternal) as List<CachedBoundaryRegion>

        return candidates.filter { it.geom.intersects(bbox) }
    }

    /**
     * 특정 좌표를 포함하는 지역 반환
     */
    fun findContaining(lng: Double, lat: Double, level: RegionLevel): CachedBoundaryRegion? {
        if (!::treesByLevel.isInitialized) return null

        val tree = treesByLevel[level] ?: return null
        val point = geometryFactory.createPoint(Coordinate(lng, lat))

        @Suppress("UNCHECKED_CAST")
        val candidates = tree.query(point.envelopeInternal) as List<CachedBoundaryRegion>

        return candidates.find { it.geom.contains(point) }
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

    /**
     * 지역 코드로 지역 정보 조회
     */
    fun getByRegionCode(regionCode: String): CachedBoundaryRegion? {
        if (!::regions.isInitialized) return null
        return regions.find { it.regionCode == regionCode }
    }

    /**
     * 지역 코드 목록으로 지역 정보 조회
     */
    fun getByRegionCodes(regionCodes: Collection<String>): List<CachedBoundaryRegion> {
        if (!::regions.isInitialized) return emptyList()
        val codeSet = regionCodes.toSet()
        return regions.filter { it.regionCode in codeSet }
    }
}
