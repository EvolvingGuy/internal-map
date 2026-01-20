package com.sanghoon.jvm_jst.rds.service

import com.sanghoon.jvm_jst.rds.cache.PnuLandCacheService
import com.sanghoon.jvm_jst.rds.repository.LandCharacteristicRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 토지특성 조회 서비스
 *
 * PNU Set을 받아서:
 * 1. 캐시 조회
 * 2. 캐시 미스 → DB 조회
 * 3. DB 결과 캐시 저장
 * 4. 전체 결과 반환
 */
@Service
class LandCharacteristicService(
    private val cacheService: PnuLandCacheService,
    private val repository: LandCharacteristicRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * PNU 목록으로 토지특성 조회 (캐시 + DB)
     * @return Map<pnu, PnuLandData>
     */
    fun getByPnus(pnus: Set<String>): Map<String, PnuLandData> {
        if (pnus.isEmpty()) return emptyMap()

        val startTime = System.currentTimeMillis()

        // 1. 캐시 조회
        val cached = cacheService.multiGet(pnus)
        val cachedPnus = cached.keys
        val missPnus = pnus - cachedPnus

        // 2. 캐시 미스 → DB 조회
        val fromDb = if (missPnus.isNotEmpty()) {
            val rows = repository.findByPnus(missPnus)
            rows.associate { row ->
                row.pnu to PnuLandData.fromWkb(
                    pnu = row.pnu,
                    geometryWkb = row.geometryWkb,
                    centerWkb = row.centerWkb,
                    area = row.area,
                    isDonut = row.isDonut
                )
            }
        } else {
            emptyMap()
        }

        // 3. DB 결과 캐시 저장
        if (fromDb.isNotEmpty()) {
            cacheService.multiSet(fromDb)
        }

        // 4. 전체 결과 병합
        val result = cached + fromDb

        val elapsedMs = System.currentTimeMillis() - startTime
        log.info("[LandCharacteristic] requested={}, cacheHit={}, cacheMiss={}, dbFound={}, elapsed={}ms",
            pnus.size, cachedPnus.size, missPnus.size, fromDb.size, elapsedMs)

        return result
    }
}
