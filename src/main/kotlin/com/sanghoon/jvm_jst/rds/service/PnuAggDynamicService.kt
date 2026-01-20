package com.sanghoon.jvm_jst.rds.service

import com.sanghoon.jvm_jst.rds.cache.AggCacheData
import com.sanghoon.jvm_jst.rds.cache.BoundaryRegionCacheService
import com.sanghoon.jvm_jst.rds.cache.PnuAggCacheService
import com.sanghoon.jvm_jst.rds.common.BBox
import com.sanghoon.jvm_jst.rds.common.H3Util
import com.sanghoon.jvm_jst.rds.common.RegionLevel
import com.sanghoon.jvm_jst.rds.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 가변형 행정구역 그루핑 응답 DTO
 */
data class DynamicRegionResponse(
    val code: Long,
    val name: String?,
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
    private val boundaryRegionCacheService: BoundaryRegionCacheService,
    private val emd11Repository: PnuAggEmd11Repository,
    private val emd10Repository: PnuAggEmd10Repository,
    private val emd09Repository: PnuAggEmd09Repository,
    private val sgg08Repository: PnuAggSgg08Repository,
    private val sgg07Repository: PnuAggSgg07Repository,
    private val sd06Repository: PnuAggSd06Repository,
    private val sd05Repository: PnuAggSd05Repository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // EMD (읍면동) - excludeRi=true: 리 제외하고 읍면동 단위로 그루핑
    fun getEmd11(bbox: BBox): DynamicResponse = fetchDynamicEmd(bbox, 11, PnuAggCacheService.PREFIX_EMD_11, excludeRi = true, codeLength = 8) { h3Indexes ->
        emd11Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getEmd10(bbox: BBox): DynamicResponse = fetchDynamicEmd(bbox, 10, PnuAggCacheService.PREFIX_EMD_10, excludeRi = true, codeLength = 8) { h3Indexes ->
        emd10Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getEmd09(bbox: BBox): DynamicResponse = fetchDynamicEmd(bbox, 9, PnuAggCacheService.PREFIX_EMD_09, excludeRi = true, codeLength = 8) { h3Indexes ->
        emd09Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    // SGG (시군구)
    fun getSgg08(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 8, PnuAggCacheService.PREFIX_SGG_08, codeLength = 5) { h3Indexes ->
        sgg08Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getSgg07(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 7, PnuAggCacheService.PREFIX_SGG_07, codeLength = 5) { h3Indexes ->
        sgg07Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    // SD (시도)
    fun getSd06(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 6, PnuAggCacheService.PREFIX_SD_06, codeLength = 2) { h3Indexes ->
        sd06Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    fun getSd05(bbox: BBox): DynamicResponse = fetchDynamic(bbox, 5, PnuAggCacheService.PREFIX_SD_05, codeLength = 2) { h3Indexes ->
        sd05Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
    }

    /**
     * SGG/SD용 - 코드 변환 없이 그대로 사용
     */
    private fun fetchDynamic(
        bbox: BBox,
        resolution: Int,
        cachePrefix: String,
        codeLength: Int,
        dbFetcher: (Collection<Long>) -> List<Pair<AggCacheData, Long>>
    ): DynamicResponse = fetchDynamicInternal(bbox, resolution, cachePrefix, 1, codeLength, dbFetcher)

    /**
     * EMD용 - 리(里) 포함/제외 선택 가능
     * @param excludeRi true: 10자리 법정동코드를 8자리 읍면동코드로 변환 (리 제외)
     *                  false: 10자리 법정동코드 그대로 사용 (리 포함)
     */
    private fun fetchDynamicEmd(
        bbox: BBox,
        resolution: Int,
        cachePrefix: String,
        excludeRi: Boolean,
        codeLength: Int,
        dbFetcher: (Collection<Long>) -> List<Pair<AggCacheData, Long>>
    ): DynamicResponse = fetchDynamicInternal(bbox, resolution, cachePrefix, if (excludeRi) 100 else 1, codeLength, dbFetcher)

    private fun fetchDynamicInternal(
        bbox: BBox,
        resolution: Int,
        cachePrefix: String,
        codeDivisor: Long,
        codeLength: Int,
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
        // EMD: codeDivisor=100 → 10자리를 8자리로 (리 제외)
        // SGG/SD: codeDivisor=1 → 그대로
        val grouped = mutableMapOf<Long, GroupedData>()
        for (data in allData) {
            val groupKey = data.code / codeDivisor
            val existing = grouped[groupKey]
            if (existing != null) {
                existing.cnt += data.cnt
                existing.sumLat += data.sumLat
                existing.sumLng += data.sumLng
            } else {
                grouped[groupKey] = GroupedData(data.cnt, data.sumLat, data.sumLng)
            }
        }

        // 6. 행정구역 이름 조회
        val regionCodes = grouped.keys.map { it.toString().padStart(codeLength, '0') }
        val regionNames = boundaryRegionCacheService.multiGet(regionCodes)

        // 7. 결과 변환
        val result = grouped.map { (code, data) ->
            val codeStr = code.toString().padStart(codeLength, '0')
            DynamicRegionResponse(
                code = code,
                name = regionNames[codeStr]?.name,
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
