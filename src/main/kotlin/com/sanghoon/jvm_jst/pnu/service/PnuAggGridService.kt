package com.sanghoon.jvm_jst.pnu.service

import com.sanghoon.jvm_jst.pnu.cache.AggCacheData
import com.sanghoon.jvm_jst.pnu.cache.PnuAggCacheService
import com.sanghoon.jvm_jst.pnu.common.BBox
import com.sanghoon.jvm_jst.pnu.common.H3Util
import com.sanghoon.jvm_jst.pnu.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 그리드 셀 응답 DTO
 */
data class GridCell(
    val row: Int,
    val col: Int,
    val cnt: Int,
    val lat: Double,
    val lng: Double
)

data class GridResponse(
    val cells: List<GridCell>,
    val totalCount: Int,
    val maxCount: Int,
    val cols: Int,
    val rows: Int,
    val elapsedMs: Long
)

/**
 * PNU Agg 그리드 서비스
 *
 * 줌레벨에 따라 사용할 테이블 분기:
 * - zoom 18~22 → emd_11 (H3 res 11)
 * - zoom 16~17 → emd_10 (H3 res 10)
 * - zoom 13~15 → sgg_08 (H3 res 8)
 * - zoom 10~12 → sd_06 (H3 res 6)
 * - zoom 0~9   → sd_05 (H3 res 5)
 */
@Service
class PnuAggGridService(
    private val cacheService: PnuAggCacheService,
    private val emd11Repository: PnuAggEmd11Repository,
    private val emd10Repository: PnuAggEmd10Repository,
    private val sgg08Repository: PnuAggSgg08Repository,
    private val sd06Repository: PnuAggSd06Repository,
    private val sd05Repository: PnuAggSd05Repository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TARGET_CELL_SIZE = 450
    }

    fun getGrid(
        bbox: BBox,
        zoomLevel: Int,
        viewportWidth: Int,
        viewportHeight: Int
    ): GridResponse {
        val startTime = System.currentTimeMillis()

        // 1. 줌레벨 → 해상도/캐시프리픽스/레포지토리 결정
        val (resolution, cachePrefix, dbFetcher) = when {
            zoomLevel >= 18 -> Triple(11, PnuAggCacheService.PREFIX_EMD_11) { h3Indexes: Collection<Long> ->
                emd11Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
            }
            zoomLevel >= 16 -> Triple(10, PnuAggCacheService.PREFIX_EMD_10) { h3Indexes: Collection<Long> ->
                emd10Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
            }
            zoomLevel >= 13 -> Triple(8, PnuAggCacheService.PREFIX_SGG_08) { h3Indexes: Collection<Long> ->
                sgg08Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
            }
            zoomLevel >= 10 -> Triple(6, PnuAggCacheService.PREFIX_SD_06) { h3Indexes: Collection<Long> ->
                sd06Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
            }
            else -> Triple(5, PnuAggCacheService.PREFIX_SD_05) { h3Indexes: Collection<Long> ->
                sd05Repository.findByH3Indexes(h3Indexes).map { AggCacheData(it.code, it.cnt, it.sumLat, it.sumLng) to it.h3Index }
            }
        }

        // 2. 그리드 크기 계산
        val cols = maxOf(1, viewportWidth / TARGET_CELL_SIZE)
        val rows = maxOf(1, viewportHeight / TARGET_CELL_SIZE)
        val cellWidth = (bbox.neLng - bbox.swLng) / cols
        val cellHeight = (bbox.neLat - bbox.swLat) / rows

        // 3. bbox → H3 인덱스
        val h3Start = System.currentTimeMillis()
        val h3Indexes = H3Util.bboxToH3Indexes(bbox, resolution)
        val h3Time = System.currentTimeMillis() - h3Start

        if (h3Indexes.isEmpty()) {
            return GridResponse(emptyList(), 0, 0, cols, rows, 0)
        }

        // 4. 캐시 조회
        val cacheStart = System.currentTimeMillis()
        val cached = cacheService.multiGet(cachePrefix, h3Indexes)
        val cacheGetTime = System.currentTimeMillis() - cacheStart
        val cachedH3s = cached.keys
        val missingH3s = h3Indexes.filter { it !in cachedH3s }

        // 5. 캐시 미스 → DB 조회
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

        // 6. 그리드에 데이터 할당
        val gridData = mutableMapOf<String, GridAggData>()

        for (h3Index in h3Indexes) {
            val dataList = cached[h3Index] ?: fromDb[h3Index] ?: continue
            for (data in dataList) {
                if (data.cnt == 0) continue
                val lat = data.sumLat / data.cnt
                val lng = data.sumLng / data.cnt

                // bbox 범위 체크
                if (lat < bbox.swLat || lat > bbox.neLat || lng < bbox.swLng || lng > bbox.neLng) continue

                // 그리드 셀 계산 (row: 위→아래 화면 기준)
                val col = ((lng - bbox.swLng) / cellWidth).toInt().coerceIn(0, cols - 1)
                val row = ((bbox.neLat - lat) / cellHeight).toInt().coerceIn(0, rows - 1)
                val gridKey = "${row}_${col}"

                val existing = gridData[gridKey]
                if (existing != null) {
                    existing.cnt += data.cnt
                    existing.sumLat += data.sumLat
                    existing.sumLng += data.sumLng
                } else {
                    gridData[gridKey] = GridAggData(row, col, data.cnt, data.sumLat, data.sumLng)
                }
            }
        }

        // 7. 결과 변환
        val cells = gridData.values.map { data ->
            GridCell(
                row = data.row,
                col = data.col,
                cnt = data.cnt,
                lat = data.sumLat / data.cnt,
                lng = data.sumLng / data.cnt
            )
        }

        val maxCount = cells.maxOfOrNull { it.cnt } ?: 0
        val totalTime = System.currentTimeMillis() - startTime

        log.info("[PnuAggGrid] zoom={}, res={}, grid={}x{}, h3={}, hit={}, miss={}, cells={} | h3={}ms, cacheGet={}ms, db={}ms, cacheSet={}ms, total={}ms",
            zoomLevel, resolution, cols, rows, h3Indexes.size, cachedH3s.size, missingH3s.size, cells.size,
            h3Time, cacheGetTime, dbTime, cacheSetTime, totalTime)

        return GridResponse(cells, cells.sumOf { it.cnt }, maxCount, cols, rows, totalTime)
    }

    private class GridAggData(val row: Int, val col: Int, var cnt: Int, var sumLat: Double, var sumLng: Double)
}
