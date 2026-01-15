package com.sanghoon.jvm_jst.pnu.service

import com.sanghoon.jvm_jst.pnu.cache.AggCacheData
import com.sanghoon.jvm_jst.pnu.cache.PnuAggCacheService
import com.sanghoon.jvm_jst.pnu.common.BBox
import com.sanghoon.jvm_jst.pnu.common.H3Util
import com.sanghoon.jvm_jst.pnu.common.RegionLevel
import com.sanghoon.jvm_jst.pnu.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 가변형 행정구역 그루핑 응답 DTO
 */
data class DynamicRegionResponse(
    val code: Long,
    val cnt: Int,
    val lat: Double,
    val lng: Double
)

data class DynamicResponse(
    val regions: List<DynamicRegionResponse>,
    val totalCount: Int,
    val elapsedMs: Long
)

/**
 * PNU Agg 가변형 서비스 - 행정구역별 그루핑
 * 캐시 레이어는 테이블 그대로 조회와 공유
 */
@Service
class PnuAggDynamicService(
    private val cacheService: PnuAggCacheService,
    private val emd11Repository: PnuAggEmd11Repository,
    private val emd10Repository: PnuAggEmd10Repository,
    private val emd09Repository: PnuAggEmd09Repository,
    private val sgg08Repository: PnuAggSgg08Repository,
    private val sgg07Repository: PnuAggSgg07Repository,
    private val sd06Repository: PnuAggSd06Repository,
    private val sd05Repository: PnuAggSd05Repository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // EMD (읍면동)
    fun getEmd11(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 11, PnuAggCacheService.PREFIX_EMD_11) { h3Indexes ->
        emd11Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getEmd10(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 10, PnuAggCacheService.PREFIX_EMD_10) { h3Indexes ->
        emd10Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getEmd09(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 9, PnuAggCacheService.PREFIX_EMD_09) { h3Indexes ->
        emd09Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    // SGG (시군구)
    fun getSgg08(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 8, PnuAggCacheService.PREFIX_SGG_08) { h3Indexes ->
        sgg08Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getSgg07(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 7, PnuAggCacheService.PREFIX_SGG_07) { h3Indexes ->
        sgg07Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    // SD (시도)
    fun getSd06(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 6, PnuAggCacheService.PREFIX_SD_06) { h3Indexes ->
        sd06Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getSd05(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 5, PnuAggCacheService.PREFIX_SD_05) { h3Indexes ->
        sd05Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    private fun fetchDynamic(
        bbox: BBox,
        resolution: Int,
        cachePrefix: String,
        dbFetcher: (Collection<Long>) -> List<Pair<AggCacheData, Long>>
    ): DynamicResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox → H3 인덱스
        val h3Indexes = H3Util.bboxToH3Indexes(bbox, resolution)
        val h3Time = System.currentTimeMillis() - startTime
        if (h3Indexes.isEmpty()) {
            return DynamicResponse(emptyList(), 0, 0)
        }

        // 2. 캐시 조회
        val cacheStart = System.currentTimeMillis()
        val cached = cacheService.multiGet(cachePrefix, h3Indexes)
        val cacheGetTime = System.currentTimeMillis() - cacheStart
        val cachedH3s = cached.keys
        val missingH3s = h3Indexes.filter { it !in cachedH3s }

        // 3. 캐시 미스 → DB 조회
        var dbTime = 0L
        var cacheSetTime = 0L
        val fromDb = if (missingH3s.isNotEmpty()) {
            val dbStart = System.currentTimeMillis()
            val dbResult = dbFetcher(missingH3s)
            dbTime = System.currentTimeMillis() - dbStart
            val grouped = dbResult.groupBy({ it.second }, { it.first })

            // 캐시 저장 (빈 결과도 저장)
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

        // 4. 모든 데이터 수집
        val allData = mutableListOf<AggCacheData>()
        for (h3Index in h3Indexes) {
            val dataList = cached[h3Index] ?: fromDb[h3Index] ?: continue
            allData.addAll(dataList)
        }

        // 5. 행정구역 코드별 그루핑 (합산)
        val grouped = mutableMapOf<Long, GroupedData>()
        for (data in allData) {
            val existing = grouped[data.code]
            if (existing != null) {
                existing.cnt += data.cnt
                existing.sumLat += data.sumLat
                existing.sumLng += data.sumLng
            } else {
                grouped[data.code] = GroupedData(data.cnt, data.sumLat, data.sumLng)
            }
        }

        // 6. 결과 변환
        val result = grouped.map { (code, data) ->
            DynamicRegionResponse(
                code = code,
                cnt = data.cnt,
                lat = data.sumLat / data.cnt,
                lng = data.sumLng / data.cnt
            )
        }

        val totalTime = System.currentTimeMillis() - startTime
        log.info("[PnuAggDynamic] res={}, h3={}, hit={}, miss={}, regions={} | h3={}ms, cacheGet={}ms, db={}ms, cacheSet={}ms, total={}ms",
            resolution, h3Indexes.size, cachedH3s.size, missingH3s.size, result.size,
            h3Time, cacheGetTime, dbTime, cacheSetTime, totalTime)

        return DynamicResponse(result, result.sumOf { it.cnt }, totalTime)
    }

    private class GroupedData(var cnt: Int, var sumLat: Double, var sumLng: Double)
}
