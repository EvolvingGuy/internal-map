package com.sanghoon.jvm_jst.rds.service

import com.sanghoon.jvm_jst.rds.cache.AggCacheData
import com.sanghoon.jvm_jst.rds.cache.PnuAggCacheService
import com.sanghoon.jvm_jst.rds.common.BBox
import com.sanghoon.jvm_jst.rds.common.H3Util
import com.sanghoon.jvm_jst.rds.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 테이블 그대로 조회 응답 DTO
 */
data class AggCellResponse(
    val h3Index: Long,
    val code: Long,
    val cnt: Int,
    val lat: Double,
    val lng: Double
)

data class AggResponse(
    val cells: List<AggCellResponse>,
    val totalCount: Int,
    val elapsedMs: Long
)

/**
 * PNU Agg 서비스 - 테이블 그대로 조회
 */
@Service
class PnuAggService(
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

    fun getEmd11(bbox: BBox): AggResponse = fetchAgg(bbox, 11, PnuAggCacheService.PREFIX_EMD_11) { h3Indexes ->
        emd11Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getEmd10(bbox: BBox): AggResponse = fetchAgg(bbox, 10, PnuAggCacheService.PREFIX_EMD_10) { h3Indexes ->
        emd10Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getEmd09(bbox: BBox): AggResponse = fetchAgg(bbox, 9, PnuAggCacheService.PREFIX_EMD_09) { h3Indexes ->
        emd09Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getSgg08(bbox: BBox): AggResponse = fetchAgg(bbox, 8, PnuAggCacheService.PREFIX_SGG_08) { h3Indexes ->
        sgg08Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getSgg07(bbox: BBox): AggResponse = fetchAgg(bbox, 7, PnuAggCacheService.PREFIX_SGG_07) { h3Indexes ->
        sgg07Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getSd06(bbox: BBox): AggResponse = fetchAgg(bbox, 6, PnuAggCacheService.PREFIX_SD_06) { h3Indexes ->
        sd06Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getSd05(bbox: BBox): AggResponse = fetchAgg(bbox, 5, PnuAggCacheService.PREFIX_SD_05) { h3Indexes ->
        sd05Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    private fun fetchAgg(
        bbox: BBox,
        resolution: Int,
        cachePrefix: String,
        dbFetcher: (Collection<Long>) -> List<Pair<AggCacheData, Long>>
    ): AggResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox → H3 인덱스
        val h3Indexes = H3Util.bboxToH3Indexes(bbox, resolution)
        val h3Time = System.currentTimeMillis() - startTime
        if (h3Indexes.isEmpty()) {
            return AggResponse(emptyList(), 0, 0)
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

        // 4. 결과 조합
        val allData = mutableListOf<AggCellResponse>()
        for (h3Index in h3Indexes) {
            val dataList = cached[h3Index] ?: fromDb[h3Index] ?: continue
            for (data in dataList) {
                if (data.cnt > 0) {
                    allData.add(
                        AggCellResponse(
                            h3Index = h3Index,
                            code = data.code,
                            cnt = data.cnt,
                            lat = data.sumLat / data.cnt,
                            lng = data.sumLng / data.cnt
                        )
                    )
                }
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        log.info("[PnuAgg] res={}, h3={}, hit={}, miss={}, result={} | h3={}ms, cacheGet={}ms, db={}ms, cacheSet={}ms, total={}ms",
            resolution, h3Indexes.size, cachedH3s.size, missingH3s.size, allData.size,
            h3Time, cacheGetTime, dbTime, cacheSetTime, totalTime)

        return AggResponse(allData, allData.sumOf { it.cnt }, totalTime)
    }
}
