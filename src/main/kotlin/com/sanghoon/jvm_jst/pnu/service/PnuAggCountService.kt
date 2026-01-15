package com.sanghoon.jvm_jst.pnu.service

import com.sanghoon.jvm_jst.pnu.cache.AggCacheData
import com.sanghoon.jvm_jst.pnu.cache.PnuAggCacheService
import com.sanghoon.jvm_jst.pnu.common.BBox
import com.sanghoon.jvm_jst.pnu.common.H3Util
import com.sanghoon.jvm_jst.pnu.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 카운트 응답 DTO
 */
data class CountResponse(
    val count: Int,
    val elapsedMs: Long
)

/**
 * PNU Agg 카운트 서비스
 *
 * 줌레벨에 따라 사용할 테이블 분기:
 * - zoom 16~22 → emd_10 (H3 res 10)
 * - zoom 12~15 → sgg_07 (H3 res 7)
 * - zoom 0~11  → sd_05 (H3 res 5)
 */
@Service
class PnuAggCountService(
    private val cacheService: PnuAggCacheService,
    private val emd10Repository: PnuAggEmd10Repository,
    private val sgg07Repository: PnuAggSgg07Repository,
    private val sd05Repository: PnuAggSd05Repository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getCount(bbox: BBox, zoomLevel: Int): CountResponse {
        val startTime = System.currentTimeMillis()

        // 1. 줌레벨 → 해상도/캐시프리픽스/레포지토리 결정
        val (resolution, cachePrefix, dbFetcher) = when {
            zoomLevel >= 16 -> Triple(10, PnuAggCacheService.PREFIX_EMD_10) { h3Indexes: Collection<Long> ->
                emd10Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
            }
            zoomLevel >= 12 -> Triple(7, PnuAggCacheService.PREFIX_SGG_07) { h3Indexes: Collection<Long> ->
                sgg07Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
            }
            else -> Triple(5, PnuAggCacheService.PREFIX_SD_05) { h3Indexes: Collection<Long> ->
                sd05Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
            }
        }

        // 2. bbox → H3 인덱스
        val h3Start = System.currentTimeMillis()
        val h3Indexes = H3Util.bboxToH3Indexes(bbox, resolution)
        val h3Time = System.currentTimeMillis() - h3Start

        if (h3Indexes.isEmpty()) {
            return CountResponse(0, 0)
        }

        // 3. 캐시 조회
        val cacheStart = System.currentTimeMillis()
        val cached = cacheService.multiGet(cachePrefix, h3Indexes)
        val cacheGetTime = System.currentTimeMillis() - cacheStart
        val cachedH3s = cached.keys
        val missingH3s = h3Indexes.filter { it !in cachedH3s }

        // 4. 캐시 미스 → DB 조회
        var dbTime = 0L
        var cacheSetTime = 0L
        val fromDb = if (missingH3s.isNotEmpty()) {
            val dbStart = System.currentTimeMillis()
            val dbResult = dbFetcher(missingH3s)
            dbTime = System.currentTimeMillis() - dbStart
            val grouped = dbResult.groupBy({ it.second }, { it.first })

            val cacheSetStart = System.currentTimeMillis()
            val toCache = mutableMapOf<Long, List<AggCacheData>>()
            for (h3Index in missingH3s) {
                toCache[h3Index] = grouped[h3Index] ?: emptyList()
            }
            cacheService.multiSet(cachePrefix, toCache)
            cacheSetTime = System.currentTimeMillis() - cacheSetStart

            grouped
        } else {
            emptyMap()
        }

        // 5. cnt 합산
        var totalCount = 0
        for (h3Index in h3Indexes) {
            val dataList = cached[h3Index] ?: fromDb[h3Index] ?: continue
            for (data in dataList) {
                totalCount += data.cnt
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        log.info("[PnuAggCount] zoom={}, res={}, h3={}, hit={}, miss={}, count={} | h3={}ms, cacheGet={}ms, db={}ms, cacheSet={}ms, total={}ms",
            zoomLevel, resolution, h3Indexes.size, cachedH3s.size, missingH3s.size, totalCount,
            h3Time, cacheGetTime, dbTime, cacheSetTime, totalTime)

        return CountResponse(totalCount, totalTime)
    }
}
