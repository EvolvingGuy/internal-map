package com.sanghoon.jvm_jst.h3

import com.sanghoon.jvm_jst.BoundaryRegionCache
import com.sanghoon.jvm_jst.RegionLevel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RegionCountService(
    private val regionCountRepository: RegionCountRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getEmdCounts(bbox: BBox): RegionCountResponse {
        return getCounts(bbox, RegionLevel.DONG, "emd")
    }

    fun getSggCounts(bbox: BBox): RegionCountResponse {
        return getCounts(bbox, RegionLevel.SIGUNGU, "sgg")
    }

    fun getSdCounts(bbox: BBox): RegionCountResponse {
        return getCounts(bbox, RegionLevel.SIDO, "sd")
    }

    private fun getCounts(bbox: BBox, regionLevel: RegionLevel, dbLevel: String): RegionCountResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox와 겹치는 행정구역 찾기
        val regions = BoundaryRegionCache.findIntersecting(
            bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat, regionLevel
        )
        val regionCodes = regions.map { it.regionCode }

        if (regionCodes.isEmpty()) {
            return RegionCountResponse(emptyList(), 0, 0)
        }

        // 2. DB 조회
        val rows = regionCountRepository.findByLevelAndCodes(dbLevel, regionCodes)

        // 3. 변환
        val counts = rows.map { row ->
            val code = row[0] as String
            val cnt = (row[1] as Number).toInt()
            val lat = (row[2] as Number).toDouble()
            val lng = (row[3] as Number).toDouble()
            val region = regions.find { it.regionCode == code }

            RegionCountDto(
                regionCode = code,
                regionName = region?.regionKoreanName ?: "",
                cnt = cnt,
                centerLat = lat,
                centerLng = lng
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[RegionCount] level=$dbLevel, regions=${regionCodes.size}, found=${counts.size}, time=${elapsed}ms")

        return RegionCountResponse(counts, counts.sumOf { it.cnt }, elapsed)
    }
}

data class RegionCountDto(
    val regionCode: String,
    val regionName: String,
    val cnt: Int,
    val centerLat: Double,
    val centerLng: Double
)

data class RegionCountResponse(
    val regions: List<RegionCountDto>,
    val totalCount: Int,
    val elapsedMs: Long
)
