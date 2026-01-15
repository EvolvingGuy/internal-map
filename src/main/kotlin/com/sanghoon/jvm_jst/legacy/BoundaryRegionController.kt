package com.sanghoon.jvm_jst.legacy

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// @RestController  // legacy - disabled
@RequestMapping("/api/boundary")
class BoundaryRegionController {

    @GetMapping("/search")
    fun search(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        @RequestParam level: RegionLevel,
        @RequestParam(required = false) expectedNames: Set<String> = emptySet()
    ): BoundarySearchResponse {
        val found = BoundaryRegionCache.findIntersecting(swLng, swLat, neLng, neLat, level)

        if (expectedNames.isEmpty()) {
            return BoundarySearchResponse(
                matched = found.map { it.toResponse() },
                missing = emptySet(),
                extra = emptyList()
            )
        }

        val foundNames = found.map { it.regionKoreanName }.toSet()
        val matched = found.filter { it.regionKoreanName in expectedNames }.map { it.toResponse() }
        val missing = expectedNames - foundNames  // 원본에는 있는데 파일에서 누락
        val extra = found.filter { it.regionKoreanName !in expectedNames }.map { it.toResponse() }  // 파일에는 있는데 원본에 없음

        return BoundarySearchResponse(
            matched = matched,
            missing = missing,
            extra = extra
        )
    }

    @GetMapping("/info")
    fun info(): CacheInfoResponse {
        return CacheInfoResponse(
            regionCount = BoundaryRegionCache.getRegionCount(),
            levels = BoundaryRegionCache.getLevels(),
            countByLevel = BoundaryRegionCache.getCountByLevel()
        )
    }
}

data class BoundarySearchResponse(
    val matched: List<BoundaryRegionResponse>,  // 일치: 원본에도 있고 파일에도 있음
    val missing: Set<String>,                   // 누락: 원본에는 있는데 파일에서 못 찾음
    val extra: List<BoundaryRegionResponse>     // 추가: 파일에는 있는데 원본에 없음
)

data class BoundaryRegionResponse(
    val regionCode: String,
    val regionKoreanName: String,
    val centerLng: Double,
    val centerLat: Double,
    val gubun: String
)

data class CacheInfoResponse(
    val regionCount: Int,
    val levels: Set<RegionLevel>,
    val countByLevel: Map<RegionLevel, Int>
)

private fun BoundaryRegion.toResponse() = BoundaryRegionResponse(
    regionCode = regionCode,
    regionKoreanName = regionKoreanName,
    centerLng = centerLng,
    centerLat = centerLat,
    gubun = gubun
)
