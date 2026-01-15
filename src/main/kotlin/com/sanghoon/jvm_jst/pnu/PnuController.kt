package com.sanghoon.jvm_jst.pnu

import com.sanghoon.jvm_jst.legacy.BoundaryRegionCache
import com.sanghoon.jvm_jst.legacy.RegionLevel
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pnu")
class PnuController(
    private val pnuCacheService: PnuCacheService,
    private val warmupRunner: PnuCacheWarmupRunner
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * bbox 내 PNU 카운트 조회 (트리밍)
     * - regionCodes로 Redis/DB에서 조회
     * - bbox 안에 center가 포함된 것만 필터
     * - regionCode별 카운트 + 동 중심점 반환
     */
    @PostMapping("/count")
    fun countInBbox(@RequestBody request: PnuCountRequest): Map<String, PnuCountResult> {
        val totalStart = System.currentTimeMillis()

        // 1. Redis/DB 조회
        val step1Start = System.currentTimeMillis()
        val allData = pnuCacheService.getByRegionCodes(request.regionCodes)
        val step1Time = System.currentTimeMillis() - step1Start

        // 2. 동 정보 조회 (STRtree)
        val step2Start = System.currentTimeMillis()
        val regions = BoundaryRegionCache.findIntersecting(
            request.swLng, request.swLat, request.neLng, request.neLat, RegionLevel.DONG
        )
        val regionMap = regions.associate { it.regionCode to it }
        val step2Time = System.currentTimeMillis() - step2Start

        // 3. bbox 필터링 + PNU 평균 중심점 계산
        val step3Start = System.currentTimeMillis()
        val result = allData.mapValues { (regionCode, list) ->
            val filtered = list.filter { dto ->
                val (lat, lng) = dto.decodeCoordinate()
                lng >= request.swLng && lng <= request.neLng &&
                lat >= request.swLat && lat <= request.neLat
            }
            val region = regionMap[regionCode]

            // PNU 평균 중심점
            val avgLng = if (filtered.isNotEmpty()) filtered.map { it.decodeLng() }.average() else 0.0
            val avgLat = if (filtered.isNotEmpty()) filtered.map { it.decodeLat() }.average() else 0.0

            PnuCountResult(
                count = filtered.size,
                centerLng = avgLng,
                centerLat = avgLat,
                name = region?.regionKoreanName ?: ""
            )
        }
        val step3Time = System.currentTimeMillis() - step3Start

        val totalTime = System.currentTimeMillis() - totalStart
        log.info("[PNU Count] regions=${request.regionCodes.size}, total=${totalTime}ms | Redis/DB=${step1Time}ms, STRtree=${step2Time}ms, Filter=${step3Time}ms")

        return result
    }

    /**
     * bbox 내 PNU 토탈 카운트만 반환
     */
    @PostMapping("/count/total")
    fun countTotalInBbox(@RequestBody request: PnuCountRequest): PnuTotalCountResult {
        val totalStart = System.currentTimeMillis()

        val allData = pnuCacheService.getByRegionCodes(request.regionCodes)

        var total = 0
        for ((_, list) in allData) {
            for (dto in list) {
                val (lat, lng) = dto.decodeCoordinate()
                if (lng >= request.swLng && lng <= request.neLng &&
                    lat >= request.swLat && lat <= request.neLat) {
                    total++
                }
            }
        }

        val totalTime = System.currentTimeMillis() - totalStart
        log.info("[PNU Total] regions=${request.regionCodes.size}, count=$total, time=${totalTime}ms")

        return PnuTotalCountResult(total)
    }

    /**
     * 캐시 무효화 (관리용)
     */
    @DeleteMapping("/cache/{regionCode}")
    fun invalidateCache(@PathVariable regionCode: String) {
        pnuCacheService.invalidate(regionCode)
    }

    @DeleteMapping("/cache")
    fun invalidateCaches(@RequestBody request: PnuInvalidateRequest) {
        pnuCacheService.invalidateAll(request.regionCodes)
    }

    /**
     * 전체 시도 캐시 워밍업
     */
    @PostMapping("/cache/warmup/all")
    fun warmupAll(): WarmupResult {
        return warmupRunner.warmupAll()
    }

    /**
     * 특정 시도 코드들만 캐시 워밍업
     */
    @PostMapping("/cache/warmup")
    fun warmup(@RequestBody request: WarmupRequest): WarmupResult {
        return warmupRunner.warmupBySido(request.sidoCodes.toSet())
    }
}

data class PnuCountRequest(
    val regionCodes: List<String>,
    val swLng: Double,
    val swLat: Double,
    val neLng: Double,
    val neLat: Double
)

data class PnuInvalidateRequest(
    val regionCodes: List<String>
)

data class PnuCountResult(
    val count: Int,
    val centerLng: Double,
    val centerLat: Double,
    val name: String
)

data class PnuTotalCountResult(
    val total: Int
)

data class WarmupRequest(
    val sidoCodes: List<String>
)
