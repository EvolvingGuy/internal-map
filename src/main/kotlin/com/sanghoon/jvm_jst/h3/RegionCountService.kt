package com.sanghoon.jvm_jst.h3

import com.sanghoon.jvm_jst.region.RegionNameCache
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RegionCountService(
    private val regionCountCache: RegionCountCache,
    private val regionNameCache: RegionNameCache
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getEmdCounts(bbox: BBox): RegionCountResponse {
        val startTime = System.currentTimeMillis()

        // bbox 내 데이터 조회 (캐시에서 좌표 필터링)
        val data = regionCountCache.getEmdByBbox(bbox)

        if (data.isEmpty()) {
            return RegionCountResponse(emptyList(), 0, 0)
        }

        // 행정구역명 조회
        val regionNames = regionNameCache.getNames(data.keys)

        // 결과 변환
        val counts = data.map { (code, d) ->
            RegionCountDto(
                regionCode = code,
                regionName = regionNames[code] ?: "",
                cnt = d.cnt,
                centerLat = d.centerLat,
                centerLng = d.centerLng
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[RegionCount] level=emd, found={}, time={}ms", counts.size, elapsed)

        return RegionCountResponse(counts, counts.sumOf { it.cnt }, elapsed)
    }

    fun getSggCounts(bbox: BBox): RegionCountResponse {
        val startTime = System.currentTimeMillis()

        val data = regionCountCache.getSggByBbox(bbox)

        if (data.isEmpty()) {
            return RegionCountResponse(emptyList(), 0, 0)
        }

        val regionNames = regionNameCache.getNames(data.keys)

        val counts = data.map { (code, d) ->
            RegionCountDto(
                regionCode = code,
                regionName = regionNames[code] ?: "",
                cnt = d.cnt,
                centerLat = d.centerLat,
                centerLng = d.centerLng
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[RegionCount] level=sgg, found={}, time={}ms", counts.size, elapsed)

        return RegionCountResponse(counts, counts.sumOf { it.cnt }, elapsed)
    }

    fun getSdCounts(bbox: BBox): RegionCountResponse {
        val startTime = System.currentTimeMillis()

        val data = regionCountCache.getSdByBbox(bbox)

        if (data.isEmpty()) {
            return RegionCountResponse(emptyList(), 0, 0)
        }

        val regionNames = regionNameCache.getNames(data.keys)

        val counts = data.map { (code, d) ->
            RegionCountDto(
                regionCode = code,
                regionName = regionNames[code] ?: "",
                cnt = d.cnt,
                centerLat = d.centerLat,
                centerLng = d.centerLng
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[RegionCount] level=sd, found={}, time={}ms", counts.size, elapsed)

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
