package com.sanghoon.jvm_jst.h3

import com.sanghoon.jvm_jst.BoundaryRegionCache
import com.sanghoon.jvm_jst.RegionLevel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class H3AggService(
    private val h3CacheService: H3CacheService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ===================== H3 격자 단위 조회 =====================

    /**
     * bbox 내 H3 셀 조회 (읍면동 레벨, res 10)
     */
    fun getEmdCells(bbox: BBox): H3AggResponse {
        val startTime = System.currentTimeMillis()

        val regions = BoundaryRegionCache.findIntersecting(
            bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat, RegionLevel.DONG
        )
        val regionCodes = regions.map { it.regionCode }

        if (regionCodes.isEmpty()) {
            return H3AggResponse(emptyList(), 0, 0)
        }

        val cached = h3CacheService.getEmdByRegionCodes(regionCodes)
        val cells = filterAndConvert(cached, bbox)

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3 Emd] regions=${regionCodes.size}, cells=${cells.size}, time=${elapsed}ms")

        return H3AggResponse(cells, cells.sumOf { it.cnt }, elapsed)
    }

    /**
     * bbox 내 H3 셀 조회 (시군구 레벨, res 8)
     */
    fun getSggCells(bbox: BBox): H3AggResponse {
        val startTime = System.currentTimeMillis()

        val regions = BoundaryRegionCache.findIntersecting(
            bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat, RegionLevel.SIGUNGU
        )
        val sggCodes = regions.map { it.regionCode }

        if (sggCodes.isEmpty()) {
            return H3AggResponse(emptyList(), 0, 0)
        }

        val cached = h3CacheService.getSggByCodes(sggCodes)
        val cells = filterAndConvert(cached, bbox)

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3 Sgg] regions=${sggCodes.size}, cells=${cells.size}, time=${elapsed}ms")

        return H3AggResponse(cells, cells.sumOf { it.cnt }, elapsed)
    }

    /**
     * bbox 내 H3 셀 조회 (시도 레벨, res 5)
     */
    fun getSdCells(bbox: BBox): H3AggResponse {
        val startTime = System.currentTimeMillis()

        val regions = BoundaryRegionCache.findIntersecting(
            bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat, RegionLevel.SIDO
        )
        val sdCodes = regions.map { it.regionCode }

        if (sdCodes.isEmpty()) {
            return H3AggResponse(emptyList(), 0, 0)
        }

        val cached = h3CacheService.getSdByCodes(sdCodes)
        val cells = filterAndConvert(cached, bbox)

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3 Sd] regions=${sdCodes.size}, cells=${cells.size}, time=${elapsed}ms")

        return H3AggResponse(cells, cells.sumOf { it.cnt }, elapsed)
    }

    private fun filterAndConvert(
        cached: Map<String, List<H3CellCacheDto>>,
        bbox: BBox
    ): List<H3CellDto> {
        return cached.flatMap { (regionCode, dtos) ->
            dtos.mapNotNull { dto ->
                if (dto.cnt == 0) return@mapNotNull null
                val avgLat = dto.sumLat / dto.cnt
                val avgLng = dto.sumLng / dto.cnt

                if (avgLng >= bbox.swLng && avgLng <= bbox.neLng &&
                    avgLat >= bbox.swLat && avgLat <= bbox.neLat) {
                    H3CellDto(dto.h3Index, dto.cnt, avgLat, avgLng, regionCode)
                } else null
            }
        }
    }

    // ===================== 행정구역 집계 조회 =====================

    /**
     * bbox 내 읍면동별 집계 (H3 res 10 캐시 기반)
     */
    fun getEmdAggregation(bbox: BBox): RegionAggResponse {
        val startTime = System.currentTimeMillis()

        val regions = BoundaryRegionCache.findIntersecting(
            bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat, RegionLevel.DONG
        )
        val regionCodes = regions.map { it.regionCode }

        if (regionCodes.isEmpty()) {
            return RegionAggResponse(emptyList(), 0, 0)
        }

        val cached = h3CacheService.getEmdByRegionCodes(regionCodes)
        val result = aggregateByRegion(cached, bbox, regions)

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[Region Emd] regions=${regionCodes.size}, result=${result.size}, time=${elapsed}ms")

        return RegionAggResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * bbox 내 시군구별 집계 (H3 res 8 캐시 기반)
     */
    fun getSggAggregation(bbox: BBox): RegionAggResponse {
        val startTime = System.currentTimeMillis()

        val regions = BoundaryRegionCache.findIntersecting(
            bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat, RegionLevel.SIGUNGU
        )
        val sggCodes = regions.map { it.regionCode }

        if (sggCodes.isEmpty()) {
            return RegionAggResponse(emptyList(), 0, 0)
        }

        val cached = h3CacheService.getSggByCodes(sggCodes)
        val result = aggregateByRegion(cached, bbox, regions)

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[Region Sgg] regions=${sggCodes.size}, result=${result.size}, time=${elapsed}ms")

        return RegionAggResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * bbox 내 시도별 집계 (H3 res 5 캐시 기반)
     */
    fun getSdAggregation(bbox: BBox): RegionAggResponse {
        val startTime = System.currentTimeMillis()

        val regions = BoundaryRegionCache.findIntersecting(
            bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat, RegionLevel.SIDO
        )
        val sdCodes = regions.map { it.regionCode }

        if (sdCodes.isEmpty()) {
            return RegionAggResponse(emptyList(), 0, 0)
        }

        val cached = h3CacheService.getSdByCodes(sdCodes)
        val result = aggregateByRegion(cached, bbox, regions)

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[Region Sd] regions=${sdCodes.size}, result=${result.size}, time=${elapsed}ms")

        return RegionAggResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    private fun aggregateByRegion(
        cached: Map<String, List<H3CellCacheDto>>,
        bbox: BBox,
        regions: List<com.sanghoon.jvm_jst.BoundaryRegion>
    ): List<RegionAggDto> {
        return cached.mapNotNull { (regionCode, dtos) ->
            var totalCnt = 0
            var totalSumLat = 0.0
            var totalSumLng = 0.0

            for (dto in dtos) {
                if (dto.cnt == 0) continue
                val avgLat = dto.sumLat / dto.cnt
                val avgLng = dto.sumLng / dto.cnt

                if (avgLng >= bbox.swLng && avgLng <= bbox.neLng &&
                    avgLat >= bbox.swLat && avgLat <= bbox.neLat) {
                    totalCnt += dto.cnt
                    totalSumLat += dto.sumLat
                    totalSumLng += dto.sumLng
                }
            }

            if (totalCnt == 0) return@mapNotNull null

            val region = regions.find { it.regionCode == regionCode }
            RegionAggDto(
                regionCode = regionCode,
                regionName = region?.regionKoreanName ?: "",
                cnt = totalCnt,
                centerLat = totalSumLat / totalCnt,
                centerLng = totalSumLng / totalCnt
            )
        }
    }
}

data class BBox(
    val swLng: Double,
    val swLat: Double,
    val neLng: Double,
    val neLat: Double
)

// H3 격자 단위 응답
data class H3CellDto(
    val h3Index: String,
    val cnt: Int,
    val lat: Double,
    val lng: Double,
    val regionCode: String
)

data class H3AggResponse(
    val cells: List<H3CellDto>,
    val totalCount: Int,
    val elapsedMs: Long
)

// 행정구역 집계 응답
data class RegionAggDto(
    val regionCode: String,
    val regionName: String,
    val cnt: Int,
    val centerLat: Double,
    val centerLng: Double
)

data class RegionAggResponse(
    val regions: List<RegionAggDto>,
    val totalCount: Int,
    val elapsedMs: Long
)
