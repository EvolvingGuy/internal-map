package com.sanghoon.jvm_jst.pnu.service

import com.sanghoon.jvm_jst.pnu.cache.BoundaryRegionCacheService
import com.sanghoon.jvm_jst.pnu.cache.PnuAggCacheService
import com.sanghoon.jvm_jst.pnu.common.BBox
import com.sanghoon.jvm_jst.pnu.common.H3Util
import com.sanghoon.jvm_jst.pnu.repository.PnuAggEmd10Repository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 중심점 행정구역 인디케이터 응답 DTO
 */
data class CenterIndicatorResponse(
    val indicator: String?,      // "서울특별시 > 중구 > 신당동" 형태
    val regionCode: String?,     // 10자리 법정동 코드
    val elapsedMs: Long
)

/**
 * 화면 중심점 기준 행정구역 인디케이터 서비스
 *
 * 플로우:
 * 1. bbox 중심점 계산
 * 2. H3 resolution 10 인덱스 조회
 * 3. emd_10 캐시에서 region code 조회
 * 4. boundary_region 캐시에서 fullName 조회
 * 5. 공백 → " > " 변환
 */
@Service
class CenterIndicatorService(
    private val aggCacheService: PnuAggCacheService,
    private val boundaryRegionCacheService: BoundaryRegionCacheService,
    private val emd10Repository: PnuAggEmd10Repository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val RESOLUTION = 10
    }

    fun getIndicator(bbox: BBox): CenterIndicatorResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox 중심점 계산
        val centerLat = (bbox.swLat + bbox.neLat) / 2
        val centerLng = (bbox.swLng + bbox.neLng) / 2

        // 2. 중심점 → H3 resolution 10 인덱스
        val h3Index = H3Util.latLngToH3(centerLat, centerLng, RESOLUTION)

        // 3. emd_10 캐시에서 해당 H3의 데이터 조회
        val cached = aggCacheService.multiGet(PnuAggCacheService.PREFIX_EMD_10, listOf(h3Index))
        val dataList = cached[h3Index] ?: run {
            // 캐시 미스 → DB 조회
            val fromDb = emd10Repository.findByH3Indexes(listOf(h3Index))
            if (fromDb.isEmpty()) {
                log.debug("[CenterIndicator] No data found for h3Index={}", h3Index)
                return CenterIndicatorResponse(null, null, System.currentTimeMillis() - startTime)
            }
            fromDb.map { com.sanghoon.jvm_jst.pnu.cache.AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) }
        }

        if (dataList.isEmpty()) {
            return CenterIndicatorResponse(null, null, System.currentTimeMillis() - startTime)
        }

        // 4. 중심점에 가장 가까운 행정구역 선택
        val closestData = dataList.minByOrNull { data ->
            val avgLat = data.sumLat / data.cnt
            val avgLng = data.sumLng / data.cnt
            distanceSquared(centerLat, centerLng, avgLat, avgLng)
        }!!

        // 5. 10자리 법정동 코드 그대로 사용
        val regionCode = closestData.code.toString()

        // 6. boundary_region 캐시에서 fullName 조회
        val boundaryData = boundaryRegionCacheService.get(regionCode)
        val indicator = boundaryData?.fullName?.replace(" ", " > ")

        val elapsedMs = System.currentTimeMillis() - startTime
        log.info("[CenterIndicator] center=({}, {}), h3={}, regionCode={}, indicator={}, elapsed={}ms",
            centerLat, centerLng, h3Index, regionCode, indicator, elapsedMs)

        return CenterIndicatorResponse(indicator, regionCode, elapsedMs)
    }

    private fun distanceSquared(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = lat1 - lat2
        val dLng = lng1 - lng2
        return dLat * dLat + dLng * dLng
    }
}
