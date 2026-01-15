package com.sanghoon.jvm_jst.pnu.service

import com.sanghoon.jvm_jst.pnu.cache.AggCacheData
import com.sanghoon.jvm_jst.pnu.cache.PnuAggCacheService
import com.sanghoon.jvm_jst.pnu.cache.StaticRegionCacheData
import com.sanghoon.jvm_jst.pnu.common.BBox
import com.sanghoon.jvm_jst.pnu.common.H3Util
import com.sanghoon.jvm_jst.pnu.common.RegionLevel
import com.sanghoon.jvm_jst.pnu.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 고정형 행정구역 그루핑 응답 DTO
 */
data class StaticRegionResponse(
    val code: Long,
    val name: String,
    val cnt: Int,
    val centerLat: Double,
    val centerLng: Double
)

data class StaticResponse(
    val regions: List<StaticRegionResponse>,
    val totalCount: Int,
    val elapsedMs: Long
)

/**
 * PNU Agg 고정형 서비스
 *
 * 흐름:
 * 1. regionLevel → H3 resolution 결정 (emd:9, sgg:7, sd:5)
 * 2. bbox → H3 indexes
 * 3. 캐시 조회 (H3 index 단위)
 * 4. 캐시 미스 → DB 2단계 조회
 *    4-1. r3_pnu_agg_{level}_{res} 에서 H3 → region_code 매핑
 *    4-2. r3_pnu_agg_static_region 에서 전체 카운트 조회
 *    4-3. H3별로 그룹핑하여 캐시 저장
 * 5. 중복 제거 후 응답
 */
@Service
class PnuAggStaticService(
    private val cacheService: PnuAggCacheService,
    private val emd09Repository: PnuAggEmd09Repository,
    private val sgg07Repository: PnuAggSgg07Repository,
    private val sd05Repository: PnuAggSd05Repository,
    private val staticRegionRepository: PnuAggStaticRegionRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getByLevel(bbox: BBox, level: RegionLevel): StaticResponse {
        return when (level) {
            RegionLevel.EMD -> getEmd(bbox)
            RegionLevel.SGG -> getSgg(bbox)
            RegionLevel.SD -> getSd(bbox)
        }
    }

    fun getEmd(bbox: BBox): StaticResponse = fetchStatic(bbox, RegionLevel.EMD) { h3Indexes ->
        emd09Repository.findByH3Indexes(h3Indexes).map { it.code to it.h3Index }
    }

    fun getSgg(bbox: BBox): StaticResponse = fetchStatic(bbox, RegionLevel.SGG) { h3Indexes ->
        sgg07Repository.findByH3Indexes(h3Indexes).map { it.code to it.h3Index }
    }

    fun getSd(bbox: BBox): StaticResponse = fetchStatic(bbox, RegionLevel.SD) { h3Indexes ->
        sd05Repository.findByH3Indexes(h3Indexes).map { it.code to it.h3Index }
    }

    private fun fetchStatic(
        bbox: BBox,
        level: RegionLevel,
        dbFetcher: (Collection<Long>) -> List<Pair<Long, Long>> // (code, h3Index)
    ): StaticResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox → H3 인덱스
        val h3Indexes = H3Util.bboxToH3Indexes(bbox, level.staticResolution)
        val h3Time = System.currentTimeMillis() - startTime
        if (h3Indexes.isEmpty()) {
            return StaticResponse(emptyList(), 0, 0)
        }

        // 2. 캐시 조회
        val cacheStart = System.currentTimeMillis()
        val cached = cacheService.multiGetStatic(level.code, h3Indexes)
        val cacheGetTime = System.currentTimeMillis() - cacheStart
        val cachedH3s = cached.keys
        val missingH3s = h3Indexes.filter { it !in cachedH3s }

        // 3. 캐시 미스 → DB 2단계 조회
        var dbTime = 0L
        var cacheSetTime = 0L
        val fromDb = if (missingH3s.isNotEmpty()) {
            val dbStart = System.currentTimeMillis()

            // 3-1. H3 → region_code 매핑
            val codeH3Pairs = dbFetcher(missingH3s)
            val h3ToCodesMap = codeH3Pairs.groupBy({ it.second }, { it.first })
            val allCodes = codeH3Pairs.map { it.first }.toSet()

            // 3-2. region_code → static_region 조회
            val staticRegions = if (allCodes.isNotEmpty()) {
                staticRegionRepository.findByLevelAndCodes(level.code, allCodes)
                    .associateBy { it.code }
            } else {
                emptyMap()
            }
            dbTime = System.currentTimeMillis() - dbStart

            // 3-3. H3별로 그룹핑하여 캐시 저장
            val cacheSetStart = System.currentTimeMillis()
            val toCache = mutableMapOf<Long, List<StaticRegionCacheData>>()
            for (h3Index in missingH3s) {
                val codes = h3ToCodesMap[h3Index] ?: emptyList()
                val regionDataList = codes.mapNotNull { code ->
                    staticRegions[code]?.let {
                        StaticRegionCacheData(
                            code = it.code,
                            name = it.name,
                            cnt = it.cnt,
                            centerLat = it.centerLat,
                            centerLng = it.centerLng
                        )
                    }
                }
                toCache[h3Index] = regionDataList
            }
            cacheService.multiSetStatic(level.code, toCache)
            cacheSetTime = System.currentTimeMillis() - cacheSetStart

            toCache
        } else {
            emptyMap()
        }

        // 4. 모든 데이터 수집 (Set으로 중복 제거)
        val uniqueRegions = mutableMapOf<Long, StaticRegionCacheData>()
        for (h3Index in h3Indexes) {
            val dataList = cached[h3Index] ?: fromDb[h3Index] ?: continue
            for (data in dataList) {
                uniqueRegions[data.code] = data // 같은 code면 덮어씀 (값은 동일)
            }
        }

        // 5. 결과 변환
        val result = uniqueRegions.values.map { data ->
            StaticRegionResponse(
                code = data.code,
                name = data.name,
                cnt = data.cnt,
                centerLat = data.centerLat,
                centerLng = data.centerLng
            )
        }

        val totalTime = System.currentTimeMillis() - startTime
        log.info("[PnuAggStatic] level={}, res={}, h3={}, hit={}, miss={}, regions={} | h3={}ms, cacheGet={}ms, db={}ms, cacheSet={}ms, total={}ms",
            level.code, level.staticResolution, h3Indexes.size, cachedH3s.size, missingH3s.size, result.size,
            h3Time, cacheGetTime, dbTime, cacheSetTime, totalTime)

        return StaticResponse(result, result.sumOf { it.cnt }, totalTime)
    }
}
