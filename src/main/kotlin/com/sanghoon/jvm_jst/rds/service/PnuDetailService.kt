package com.sanghoon.jvm_jst.rds.service

import com.sanghoon.jvm_jst.rds.cache.PnuH3CacheService
import com.sanghoon.jvm_jst.rds.common.BBox
import com.sanghoon.jvm_jst.rds.common.H3Util
import com.sanghoon.jvm_jst.rds.repository.PnuH310Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * PNU 상세 조회 응답 DTO
 */
data class PnuDetailResponse(
    val items: List<PnuLandData>,
    val pnuCount: Int,
    val exceeded: Boolean,
    val elapsedMs: Long
)

/**
 * PNU 상세 조회 서비스
 *
 * - PNU 갯수 2000개 초과: 빈 결과 반환
 * - H3 index별 PNU Set 조회 (캐시 + DB)
 * - PNU를 통해 연관 데이터 병렬 조회 (코루틴)
 */
@Service
class PnuDetailService(
    private val pnuH3CacheService: PnuH3CacheService,
    private val pnuH310Repository: PnuH310Repository,
    private val landCharacteristicService: LandCharacteristicService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val H3_RESOLUTION = 10
        private const val MAX_PNU_COUNT = 2000
    }

    fun getPnus(bbox: BBox): PnuDetailResponse {
        val startTime = System.currentTimeMillis()

        // bbox → H3 indexes (resolution 10)
        val h3Indexes = H3Util.bboxToH3Indexes(bbox, H3_RESOLUTION)
        if (h3Indexes.isEmpty()) {
            return PnuDetailResponse(
                items = emptyList(),
                pnuCount = 0,
                exceeded = false,
                elapsedMs = System.currentTimeMillis() - startTime
            )
        }

        // 1. 캐시 조회 (H3 → PNU)
        val cached = pnuH3CacheService.multiGet(h3Indexes)
        val cachedH3Indexes = cached.keys
        val missH3Indexes = h3Indexes - cachedH3Indexes

        // 2. 캐시 미스 → DB 조회
        val fromDb = if (missH3Indexes.isNotEmpty()) {
            val entities = pnuH310Repository.findByH3Indexes(missH3Indexes)
            entities.groupBy { it.h3Index }
                .mapValues { (_, list) -> list.map { it.pnu }.toSet() }
        } else {
            emptyMap()
        }

        // 3. DB 결과 캐시 저장 (빈 결과도 저장)
        if (missH3Indexes.isNotEmpty()) {
            val toCache = missH3Indexes.associateWith { h3Index ->
                fromDb[h3Index] ?: emptySet()
            }
            pnuH3CacheService.multiSet(toCache)
        }

        // 4. 전체 PNU Set 병합
        val allPnus = mutableSetOf<String>()
        cached.values.forEach { allPnus.addAll(it) }
        fromDb.values.forEach { allPnus.addAll(it) }

        // 5. 2000개 초과 체크
        if (allPnus.size > MAX_PNU_COUNT) {
            val elapsedMs = System.currentTimeMillis() - startTime
            log.info("[PnuDetail] EXCEEDED h3Count={}, pnuCount={}, elapsed={}ms",
                h3Indexes.size, allPnus.size, elapsedMs)
            return PnuDetailResponse(
                items = emptyList(),
                pnuCount = allPnus.size,
                exceeded = true,
                elapsedMs = elapsedMs
            )
        }

        // 6. PNU를 통해 연관 데이터 병렬 조회 (코루틴)
        val landDataMap = runBlocking(Dispatchers.IO) {
            // 나중에 다른 서비스 추가 시 async로 병렬 처리
            val landDeferred = async { landCharacteristicService.getByPnus(allPnus) }
            // val otherDeferred = async { otherService.getByPnus(allPnus) }

            landDeferred.await()
            // 나중에: merge(landDeferred.await(), otherDeferred.await())
        }

        val elapsedMs = System.currentTimeMillis() - startTime
        log.info("[PnuDetail] h3Count={}, cacheHit={}, cacheMiss={}, pnuCount={}, landCount={}, elapsed={}ms",
            h3Indexes.size, cachedH3Indexes.size, missH3Indexes.size, allPnus.size, landDataMap.size, elapsedMs)

        return PnuDetailResponse(
            items = landDataMap.values.toList(),
            pnuCount = allPnus.size,
            exceeded = false,
            elapsedMs = elapsedMs
        )
    }
}
