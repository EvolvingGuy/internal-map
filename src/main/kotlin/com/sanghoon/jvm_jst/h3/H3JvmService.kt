package com.sanghoon.jvm_jst.h3

import com.sanghoon.jvm_jst.region.RegionNameCache
import com.uber.h3core.H3Core
import com.uber.h3core.util.LatLng
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.nio.ByteBuffer

@Service
class H3JvmService(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
    private val emdRepository: H3AggEmdRepository,
    private val sggRepository: H3AggSggRepository,
    private val sdRepository: H3AggSdRepository,
    private val regionNameCache: RegionNameCache,
    private val h3Emd10Cache: H3Emd10Cache,
    private val h3Sgg8Cache: H3Sgg8Cache,
    private val h3Sd6Cache: H3Sd6Cache
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val h3 = H3Core.newInstance()

    companion object {
        private const val CACHE_PREFIX_EMD = "h3:emd:"
        private const val CACHE_PREFIX_SGG = "h3:sgg:"
        private const val CACHE_PREFIX_SD = "h3:sd:"
        private const val CACHE_PREFIX_EMD_REGION = "h3:emd:region:"
        private const val CACHE_PREFIX_EMD10_REGION = "h3:emd10:region:"
        private const val CACHE_PREFIX_EMD10_CELL = "h3:emd10:cell:"
        private const val RESOLUTION_EMD = 10
        private const val RESOLUTION_SGG = 8
        private const val RESOLUTION_SGG8 = 8
        private const val RESOLUTION_SD = 5
        private const val RESOLUTION_SD6 = 6
    }

    /**
     * 읍면동 집계 (H3 res 10, Redis 캐시)
     */
    fun getEmdAggregation(bbox: BBox): H3JvmResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox → H3 인덱스 (순수 연산)
        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_EMD)
        log.debug("[H3Jvm Emd] h3Indexes=${h3Indexes.size}")

        if (h3Indexes.isEmpty()) {
            return H3JvmResponse(emptyList(), 0, 0)
        }

        // 2. Redis 캐시 조회 + DB fallback
        val cells = getCellsWithCache(h3Indexes, CACHE_PREFIX_EMD) { missingIndexes ->
            emdRepository.findByH3Indexes(missingIndexes).map { row ->
                H3CellData(
                    h3Index = row[0] as String,
                    cnt = (row[1] as Number).toInt(),
                    sumLat = (row[2] as Number).toDouble(),
                    sumLng = (row[3] as Number).toDouble()
                )
            }
        }

        // 3. bbox 내 필터링 + 집계
        val filtered = filterByBbox(cells, bbox)
        val result = aggregateCells(filtered)

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3Jvm Emd] indexes=${h3Indexes.size}, cells=${cells.size}, filtered=${filtered.size}, time=${elapsed}ms")

        return H3JvmResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * 시군구 집계 (H3 res 8, Redis 캐시)
     */
    fun getSggAggregation(bbox: BBox): H3JvmResponse {
        val startTime = System.currentTimeMillis()

        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_SGG)

        if (h3Indexes.isEmpty()) {
            return H3JvmResponse(emptyList(), 0, 0)
        }

        val cells = getCellsWithCache(h3Indexes, CACHE_PREFIX_SGG) { missingIndexes ->
            sggRepository.findByH3Indexes(missingIndexes).map { row ->
                H3CellData(
                    h3Index = row[0] as String,
                    cnt = (row[1] as Number).toInt(),
                    sumLat = (row[2] as Number).toDouble(),
                    sumLng = (row[3] as Number).toDouble()
                )
            }
        }

        val filtered = filterByBbox(cells, bbox)
        val result = aggregateCells(filtered)

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3Jvm Sgg] indexes=${h3Indexes.size}, cells=${cells.size}, filtered=${filtered.size}, time=${elapsed}ms")

        return H3JvmResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * 시도 집계 (H3 res 5, Redis 캐시)
     */
    fun getSdAggregation(bbox: BBox): H3JvmResponse {
        val startTime = System.currentTimeMillis()

        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_SD)

        if (h3Indexes.isEmpty()) {
            return H3JvmResponse(emptyList(), 0, 0)
        }

        val cells = getCellsWithCache(h3Indexes, CACHE_PREFIX_SD) { missingIndexes ->
            sdRepository.findByH3Indexes(missingIndexes).map { row ->
                H3CellData(
                    h3Index = row[0] as String,
                    cnt = (row[1] as Number).toInt(),
                    sumLat = (row[2] as Number).toDouble(),
                    sumLng = (row[3] as Number).toDouble()
                )
            }
        }

        val filtered = filterByBbox(cells, bbox)
        val result = aggregateCells(filtered)

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3Jvm Sd] indexes=${h3Indexes.size}, cells=${cells.size}, filtered=${filtered.size}, time=${elapsed}ms")

        return H3JvmResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * 읍면동별 집계 (bbox → H3 indexes → Redis 캐시 → DB fallback → bjdong 집계)
     */
    fun getEmdRegionAggregation(bbox: BBox): H3RegionResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox → H3 인덱스
        val h3Start = System.currentTimeMillis()
        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_EMD)
        val h3Time = System.currentTimeMillis() - h3Start

        if (h3Indexes.isEmpty()) {
            return H3RegionResponse(emptyList(), 0, 0)
        }

        // 2. Redis multiGet
        val redisStart = System.currentTimeMillis()
        val allBjdongData = mutableListOf<BjdongCellData>()
        val missingIndexes = mutableListOf<String>()

        val keys = h3Indexes.map { "$CACHE_PREFIX_EMD_REGION$it" }
        val cached = redisTemplate.opsForValue().multiGet(keys) ?: emptyList()
        val redisTime = System.currentTimeMillis() - redisStart

        // Redis 전송량 계산
        var redisBytes = 0L
        val parseStart = System.currentTimeMillis()
        h3Indexes.forEachIndexed { idx, h3Index ->
            val data = cached.getOrNull(idx)
            if (data != null && data.isNotEmpty()) {
                redisBytes += data.size
                allBjdongData.addAll(deserializeRegion(data))
            } else {
                missingIndexes.add(h3Index)
            }
        }
        val parseTime = System.currentTimeMillis() - parseStart

        // 3. Cache miss → DB 조회 후 캐시 저장
        var dbTime = 0L
        var cacheWriteTime = 0L
        if (missingIndexes.isNotEmpty()) {
            val dbStart = System.currentTimeMillis()
            val rows = emdRepository.findByH3IndexesWithBjdong(missingIndexes)
            dbTime = System.currentTimeMillis() - dbStart

            // H3 index별로 그룹핑
            val byH3Index = rows.groupBy { it[1] as String }

            val cacheStart = System.currentTimeMillis()
            for ((h3Index, rowList) in byH3Index) {
                val cellDataList = rowList.map { row ->
                    BjdongCellData(
                        bjdongCd = (row[0] as Number).toInt(),
                        cnt = (row[2] as Number).toShort(),
                        sumLat = (row[3] as Number).toDouble(),
                        sumLng = (row[4] as Number).toDouble()
                    )
                }
                allBjdongData.addAll(cellDataList)
                redisTemplate.opsForValue().set("$CACHE_PREFIX_EMD_REGION$h3Index", serializeRegion(cellDataList))
            }

            // DB에 없는 H3 index는 빈 캐시 저장
            val foundIndexes = byH3Index.keys
            for (h3Index in missingIndexes) {
                if (h3Index !in foundIndexes) {
                    redisTemplate.opsForValue().set("$CACHE_PREFIX_EMD_REGION$h3Index", serializeRegion(emptyList()))
                }
            }
            cacheWriteTime = System.currentTimeMillis() - cacheStart

            log.info("[H3Jvm EmdRegion] MISS: db=${dbTime}ms, cacheWrite=${cacheWriteTime}ms, rows=${rows.size}")
        }

        // 4. bjdong_cd로 그룹핑 및 집계
        val aggStart = System.currentTimeMillis()
        val grouped = mutableMapOf<Int, BjdongAggData>()
        for (data in allBjdongData) {
            val existing = grouped[data.bjdongCd]
            if (existing != null) {
                existing.cnt += data.cnt.toInt()
                existing.sumLat += data.sumLat
                existing.sumLng += data.sumLng
            } else {
                grouped[data.bjdongCd] = BjdongAggData(data.cnt.toInt(), data.sumLat, data.sumLng)
            }
        }

        // 5. 결과 변환 (중심점 계산 + 행정구역명 조회)
        val regionNames = regionNameCache.getNames(grouped.keys.map { it.toString() })
        val result = grouped.map { (bjdongCd, data) ->
            val bjdongCdStr = bjdongCd.toString()
            H3RegionCell(
                bjdongCd = bjdongCdStr,
                cnt = data.cnt,
                lat = data.sumLat / data.cnt,
                lng = data.sumLng / data.cnt
            )
        }
        val aggTime = System.currentTimeMillis() - aggStart

        val elapsed = System.currentTimeMillis() - startTime
        val cacheHit = h3Indexes.size - missingIndexes.size
        val redisKB = redisBytes / 1024.0
        log.info("[H3Jvm EmdRegion] 1.h3=${h3Time}ms → 2.redis=${redisTime}ms(${String.format("%.1f", redisKB)}KB) → 3.parse=${parseTime}ms → 4.db=${dbTime}ms → 5.cacheWrite=${cacheWriteTime}ms → 6.agg=${aggTime}ms | hit=$cacheHit, miss=${missingIndexes.size}, total=${elapsed}ms")

        return H3RegionResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * 읍면동별 집계 - H3 res 9 (bbox → H3 indexes → JVM 캐시 → bjdong 집계)
     */
    fun getEmd10RegionAggregation(bbox: BBox): H3RegionResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox → H3 인덱스 (res 9)
        val h3Start = System.currentTimeMillis()
        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_EMD)
        val h3Time = System.currentTimeMillis() - h3Start

        if (h3Indexes.isEmpty()) {
            return H3RegionResponse(emptyList(), 0, 0)
        }

        // 2. JVM 캐시 조회
        val cacheStart = System.currentTimeMillis()
        val cachedData = h3Emd10Cache.get(h3Indexes)
        val cacheTime = System.currentTimeMillis() - cacheStart

        // 3. bjdong_cd로 그룹핑 및 집계
        val aggStart = System.currentTimeMillis()
        val grouped = mutableMapOf<Int, BjdongAggData>()
        var fetchedRecords = 0
        for ((_, bjdongList) in cachedData) {
            fetchedRecords += bjdongList.size
            for (data in bjdongList) {
                val existing = grouped[data.bjdongCd]
                if (existing != null) {
                    existing.cnt += data.cnt.toInt()
                    existing.sumLat += data.sumLat
                    existing.sumLng += data.sumLng
                } else {
                    grouped[data.bjdongCd] = BjdongAggData(data.cnt.toInt(), data.sumLat, data.sumLng)
                }
            }
        }

        // 4. 결과 변환 (중심점 계산 + 행정구역명 조회)
        val result = grouped.map { (bjdongCd, data) ->
            val bjdongCdStr = bjdongCd.toString()
            H3RegionCell(
                bjdongCd = bjdongCdStr,
                cnt = data.cnt,
                lat = data.sumLat / data.cnt,
                lng = data.sumLng / data.cnt
            )
        }
        val aggTime = System.currentTimeMillis() - aggStart

        val elapsed = System.currentTimeMillis() - startTime
        val fetchedKB = (fetchedRecords * 80) / 1024.0
        log.info("[H3 Emd10Region JVM] h3=${h3Time}ms → cache=${cacheTime}ms → agg=${aggTime}ms | indexes=${h3Indexes.size}, fetched=${fetchedRecords}(${String.format("%.1f", fetchedKB)}KB), total=${elapsed}ms")

        return H3RegionResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * H3 격자 단위 집계 - res 9 (bjdong 집계 없이 격자 자체의 중심/카운트, JVM 캐시)
     */
    fun getEmd10CellAggregation(bbox: BBox): H3JvmResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox → H3 인덱스 (res 9)
        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_EMD)

        if (h3Indexes.isEmpty()) {
            return H3JvmResponse(emptyList(), 0, 0)
        }

        // 2. JVM 캐시 조회
        val cachedData = h3Emd10Cache.get(h3Indexes)

        // 3. H3 셀별로 합산
        val cells = cachedData.map { (h3Index, bjdongList) ->
            var totalCnt = 0
            var totalSumLat = 0.0
            var totalSumLng = 0.0
            for (data in bjdongList) {
                totalCnt += data.cnt
                totalSumLat += data.sumLat
                totalSumLng += data.sumLng
            }
            H3CellData(h3Index, totalCnt, totalSumLat, totalSumLng)
        }

        // 4. bbox 내 필터링 + 결과 변환
        val filtered = filterByBbox(cells, bbox)
        val result = aggregateCells(filtered)

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3 Emd10Cell JVM] indexes=${h3Indexes.size}, cells=${cells.size}, filtered=${filtered.size}, time=${elapsed}ms")

        return H3JvmResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * H3 격자를 3x3 그리드로 집계 - res 9 (JVM 캐시)
     * 화면 기준: 1|2|3 / 4|5|6 / 7|8|9 (북쪽이 위)
     * 표시 위치: 각 그리드 셀의 기하학적 중심
     */
    fun getEmd10GridAggregation(bbox: BBox): H3JvmResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox → H3 인덱스 (res 9)
        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_EMD)

        if (h3Indexes.isEmpty()) {
            return H3JvmResponse(emptyList(), 0, 0)
        }

        // 2. JVM 캐시 조회
        val cachedData = h3Emd10Cache.get(h3Indexes)

        // 3. H3 셀별로 합산
        val cells = cachedData.map { (h3Index, bjdongList) ->
            var totalCnt = 0
            var totalSumLat = 0.0
            var totalSumLng = 0.0
            for (data in bjdongList) {
                totalCnt += data.cnt
                totalSumLat += data.sumLat
                totalSumLng += data.sumLng
            }
            H3CellData(h3Index, totalCnt, totalSumLat, totalSumLng)
        }

        // 4. bbox 내 필터링
        val filtered = filterByBbox(cells, bbox)

        // 5. 3x3 그리드로 집계 (cnt + sumLat + sumLng 모두 합산)
        val gridCols = 3
        val gridRows = 3
        val cellWidth = (bbox.neLng - bbox.swLng) / gridCols
        val cellHeight = (bbox.neLat - bbox.swLat) / gridRows

        val gridData = mutableMapOf<String, GridAggData>()

        for (cell in filtered) {
            if (cell.cnt == 0) continue
            val lat = cell.sumLat / cell.cnt
            val lng = cell.sumLng / cell.cnt

            // 컬럼: 왼쪽(서) = 0, 오른쪽(동) = 2
            val col = ((lng - bbox.swLng) / cellWidth).toInt().coerceIn(0, gridCols - 1)
            // 행: 위(북) = 0, 아래(남) = 2 (화면 기준)
            val row = ((bbox.neLat - lat) / cellHeight).toInt().coerceIn(0, gridRows - 1)
            val gridKey = "${row}_${col}"

            val existing = gridData[gridKey]
            if (existing != null) {
                existing.cnt += cell.cnt
                existing.sumLat += cell.sumLat
                existing.sumLng += cell.sumLng
            } else {
                gridData[gridKey] = GridAggData(cell.cnt, cell.sumLat, cell.sumLng)
            }
        }

        // 6. 결과 변환 (그리드 내 데이터 가중 평균 위치)
        val result = gridData.map { (gridKey, data) ->
            H3JvmCell(
                h3Index = gridKey,
                cnt = data.cnt,
                lat = data.sumLat / data.cnt,
                lng = data.sumLng / data.cnt
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3 Emd10Grid JVM] indexes=${h3Indexes.size}, cells=${filtered.size}, grids=${result.size}, time=${elapsed}ms")

        return H3JvmResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * 시군구별 집계 - H3 res 8 (bbox → H3 indexes → JVM 캐시 → sgg 집계)
     */
    fun getSgg8RegionAggregation(bbox: BBox): H3RegionResponse {
        val startTime = System.currentTimeMillis()

        val h3Start = System.currentTimeMillis()
        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_SGG8)
        val h3Time = System.currentTimeMillis() - h3Start

        if (h3Indexes.isEmpty()) {
            return H3RegionResponse(emptyList(), 0, 0)
        }

        val cacheStart = System.currentTimeMillis()
        val cachedData = h3Sgg8Cache.get(h3Indexes)
        val cacheTime = System.currentTimeMillis() - cacheStart

        val aggStart = System.currentTimeMillis()
        val grouped = mutableMapOf<String, SggAggData>()
        var fetchedRecords = 0
        for ((_, sggList) in cachedData) {
            fetchedRecords += sggList.size
            for (data in sggList) {
                val existing = grouped[data.sggCd]
                if (existing != null) {
                    existing.cnt += data.cnt
                    existing.sumLat += data.sumLat
                    existing.sumLng += data.sumLng
                } else {
                    grouped[data.sggCd] = SggAggData(data.cnt, data.sumLat, data.sumLng)
                }
            }
        }

        val regionNames = regionNameCache.getNames(grouped.keys)
        val result = grouped.map { (sggCd, data) ->
            H3RegionCell(
                bjdongCd = sggCd,
                cnt = data.cnt,
                lat = data.sumLat / data.cnt,
                lng = data.sumLng / data.cnt
            )
        }
        val aggTime = System.currentTimeMillis() - aggStart

        val elapsed = System.currentTimeMillis() - startTime
        val fetchedKB = (fetchedRecords * 70) / 1024.0
        log.info("[H3 Sgg8Region JVM] h3=${h3Time}ms → cache=${cacheTime}ms → agg=${aggTime}ms | indexes=${h3Indexes.size}, fetched=${fetchedRecords}(${String.format("%.1f", fetchedKB)}KB), total=${elapsed}ms")

        return H3RegionResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * 시도별 집계 - H3 res 6 (bbox → H3 indexes → JVM 캐시 → sd 집계)
     */
    fun getSd6RegionAggregation(bbox: BBox): H3RegionResponse {
        val startTime = System.currentTimeMillis()

        val h3Start = System.currentTimeMillis()
        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_SD6)
        val h3Time = System.currentTimeMillis() - h3Start

        if (h3Indexes.isEmpty()) {
            return H3RegionResponse(emptyList(), 0, 0)
        }

        val cacheStart = System.currentTimeMillis()
        val cachedData = h3Sd6Cache.get(h3Indexes)
        val cacheTime = System.currentTimeMillis() - cacheStart

        val aggStart = System.currentTimeMillis()
        val grouped = mutableMapOf<String, SdAggData>()
        var fetchedRecords = 0
        for ((_, sdList) in cachedData) {
            fetchedRecords += sdList.size
            for (data in sdList) {
                val existing = grouped[data.sdCd]
                if (existing != null) {
                    existing.cnt += data.cnt
                    existing.sumLat += data.sumLat
                    existing.sumLng += data.sumLng
                } else {
                    grouped[data.sdCd] = SdAggData(data.cnt, data.sumLat, data.sumLng)
                }
            }
        }

        val regionNames = regionNameCache.getNames(grouped.keys)
        val result = grouped.map { (sdCd, data) ->
            H3RegionCell(
                bjdongCd = sdCd,
                cnt = data.cnt,
                lat = data.sumLat / data.cnt,
                lng = data.sumLng / data.cnt
            )
        }
        val aggTime = System.currentTimeMillis() - aggStart

        val elapsed = System.currentTimeMillis() - startTime
        val fetchedKB = (fetchedRecords * 60) / 1024.0
        log.info("[H3 Sd6Region JVM] h3=${h3Time}ms → cache=${cacheTime}ms → agg=${aggTime}ms | indexes=${h3Indexes.size}, fetched=${fetchedRecords}(${String.format("%.1f", fetchedKB)}KB), total=${elapsed}ms")

        return H3RegionResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * 고정 미터 단위 그리드 집계 - res 9 (JVM 캐시)
     * 뷰포트를 gridSizeMeters 단위 정사각형으로 나눠서 집계
     */
    fun getFixedGridAggregation(bbox: BBox, gridSizeMeters: Double = 300.0): H3FixedGridResponse {
        val startTime = System.currentTimeMillis()

        // 뷰포트 가로 크기 체크 (100km 제한)
        val centerLat = (bbox.swLat + bbox.neLat) / 2
        val lngDegPerMeter = 1.0 / (111000.0 * kotlin.math.cos(Math.toRadians(centerLat)))
        val viewportWidthMeters = (bbox.neLng - bbox.swLng) / lngDegPerMeter

        if (viewportWidthMeters > 100000) {
            return H3FixedGridResponse(
                cells = emptyList(),
                totalCount = 0,
                elapsedMs = 0,
                error = "뷰포트 가로가 100km를 초과합니다. 더 가까이 확대해주세요."
            )
        }

        // 1. bbox → H3 인덱스 (res 9)
        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_EMD)

        if (h3Indexes.isEmpty()) {
            return H3FixedGridResponse(emptyList(), 0, 0, null)
        }

        // 2. JVM 캐시 조회
        val cachedData = h3Emd10Cache.get(h3Indexes)

        // 3. 그리드 크기 계산 (도 단위)
        val latDegPerMeter = 1.0 / 111000.0
        val gridLatSize = gridSizeMeters * latDegPerMeter
        val gridLngSize = gridSizeMeters * lngDegPerMeter

        // 4. H3 셀별로 합산 후 그리드에 할당
        val gridData = mutableMapOf<String, FixedGridAggData>()

        for ((h3Index, bjdongList) in cachedData) {
            for (data in bjdongList) {
                if (data.cnt.toInt() == 0) continue
                val lat = data.sumLat / data.cnt
                val lng = data.sumLng / data.cnt

                // bbox 범위 체크
                if (lat < bbox.swLat || lat > bbox.neLat || lng < bbox.swLng || lng > bbox.neLng) continue

                // 그리드 셀 계산
                val gridRow = kotlin.math.floor((lat - bbox.swLat) / gridLatSize).toInt()
                val gridCol = kotlin.math.floor((lng - bbox.swLng) / gridLngSize).toInt()
                val gridKey = "${gridRow}_${gridCol}"

                val existing = gridData[gridKey]
                if (existing != null) {
                    existing.cnt += data.cnt.toInt()
                    existing.sumLat += data.sumLat
                    existing.sumLng += data.sumLng
                } else {
                    gridData[gridKey] = FixedGridAggData(
                        row = gridRow,
                        col = gridCol,
                        cnt = data.cnt.toInt(),
                        sumLat = data.sumLat,
                        sumLng = data.sumLng
                    )
                }
            }
        }

        // 5. 결과 변환 (그리드 셀 중심점)
        val result = gridData.map { (_, data) ->
            H3FixedGridCell(
                row = data.row,
                col = data.col,
                cnt = data.cnt,
                lat = data.sumLat / data.cnt,
                lng = data.sumLng / data.cnt,
                // 그리드 셀 경계 (디버깅/시각화용)
                gridSwLat = bbox.swLat + data.row * gridLatSize,
                gridSwLng = bbox.swLng + data.col * gridLngSize,
                gridNeLat = bbox.swLat + (data.row + 1) * gridLatSize,
                gridNeLng = bbox.swLng + (data.col + 1) * gridLngSize
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3 FixedGrid] gridSize={}m, h3={}, grids={}, time={}ms",
            gridSizeMeters.toInt(), h3Indexes.size, result.size, elapsed)

        return H3FixedGridResponse(result, result.sumOf { it.cnt }, elapsed, null)
    }

    /**
     * 뷰포트 기반 동적 그리드 집계 - res 9 (JVM 캐시)
     * 뷰포트 픽셀 크기에 따라 격자 개수가 동적으로 결정됨
     * @param bbox 바운딩박스
     * @param viewportWidth 뷰포트 가로 픽셀
     * @param viewportHeight 뷰포트 세로 픽셀
     * @param targetCellSize 셀당 목표 픽셀 (기본 450px)
     */
    fun getViewportGridAggregation(
        bbox: BBox,
        viewportWidth: Int,
        viewportHeight: Int,
        targetCellSize: Int = 450
    ): H3ViewportGridResponse {
        val startTime = System.currentTimeMillis()

        // 1. 뷰포트 픽셀 기반 격자 개수 계산
        val cols = maxOf(1, (viewportWidth.toDouble() / targetCellSize).toInt())
        val rows = maxOf(1, (viewportHeight.toDouble() / targetCellSize).toInt())

        // 2. bbox → H3 인덱스 (res 10, 약 66m)
        val h3Indexes = bboxToH3Indexes(bbox, RESOLUTION_EMD)

        if (h3Indexes.isEmpty()) {
            return H3ViewportGridResponse(emptyList(), 0, 0, cols, rows, null)
        }

        // 3. JVM 캐시 조회 (res 10)
        val cachedData = h3Emd10Cache.get(h3Indexes)

        // 4. 그리드 크기 계산
        val cellWidth = (bbox.neLng - bbox.swLng) / cols
        val cellHeight = (bbox.neLat - bbox.swLat) / rows

        // 5. 각 H3 셀 데이터를 그리드에 할당
        val gridData = mutableMapOf<String, ViewportGridAggData>()

        for ((_, bjdongList) in cachedData) {
            for (data in bjdongList) {
                if (data.cnt.toInt() == 0) continue
                val lat = data.sumLat / data.cnt
                val lng = data.sumLng / data.cnt

                // bbox 범위 체크
                if (lat < bbox.swLat || lat > bbox.neLat || lng < bbox.swLng || lng > bbox.neLng) continue

                // 그리드 셀 계산 (col: 왼쪽→오른쪽, row: 위→아래 화면 기준)
                val col = ((lng - bbox.swLng) / cellWidth).toInt().coerceIn(0, cols - 1)
                val row = ((bbox.neLat - lat) / cellHeight).toInt().coerceIn(0, rows - 1)
                val gridKey = "${row}_${col}"

                val existing = gridData[gridKey]
                if (existing != null) {
                    existing.cnt += data.cnt.toInt()
                    existing.sumLat += data.sumLat
                    existing.sumLng += data.sumLng
                } else {
                    gridData[gridKey] = ViewportGridAggData(row, col, data.cnt.toInt(), data.sumLat, data.sumLng)
                }
            }
        }

        // 6. 결과 변환 (가중 평균 위치)
        val result = gridData.map { (_, data) ->
            H3ViewportGridCell(
                row = data.row,
                col = data.col,
                cnt = data.cnt,
                lat = data.sumLat / data.cnt,
                lng = data.sumLng / data.cnt
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3 ViewportGrid] viewport={}x{}, targetCell={}px, grid={}x{}, h3={}, cells={}, time={}ms",
            viewportWidth, viewportHeight, targetCellSize, cols, rows, h3Indexes.size, result.size, elapsed)

        return H3ViewportGridResponse(result, result.sumOf { it.cnt }, elapsed, cols, rows, null)
    }

    private class BjdongAggData(var cnt: Int, var sumLat: Double, var sumLng: Double)
    private class SggAggData(var cnt: Int, var sumLat: Double, var sumLng: Double)
    private class SdAggData(var cnt: Int, var sumLat: Double, var sumLng: Double)
    private class GridAggData(var cnt: Int, var sumLat: Double, var sumLng: Double)
    private class FixedGridAggData(val row: Int, val col: Int, var cnt: Int, var sumLat: Double, var sumLng: Double)
    private class ViewportGridAggData(val row: Int, val col: Int, var cnt: Int, var sumLat: Double, var sumLng: Double)

    // Region 직렬화: [4 bytes count][반복: 4 bytes bjdongCd(Int) + 2 bytes cnt(Short) + 8 bytes sumLat + 8 bytes sumLng] = 22 bytes/record
    private fun serializeRegion(list: List<BjdongCellData>): ByteArray {
        val buffer = ByteBuffer.allocate(4 + list.size * 22)
        buffer.putInt(list.size)
        for (data in list) {
            buffer.putInt(data.bjdongCd)
            buffer.putShort(data.cnt)
            buffer.putDouble(data.sumLat)
            buffer.putDouble(data.sumLng)
        }
        return buffer.array()
    }

    private fun deserializeRegion(data: ByteArray): List<BjdongCellData> {
        val buffer = ByteBuffer.wrap(data)
        val count = buffer.int
        val result = ArrayList<BjdongCellData>(count)
        repeat(count) {
            val bjdongCd = buffer.int
            val cnt = buffer.short
            val sumLat = buffer.double
            val sumLng = buffer.double
            result.add(BjdongCellData(bjdongCd, cnt, sumLat, sumLng))
        }
        return result
    }

    /**
     * bbox → H3 인덱스 변환 (순수 연산)
     */
    private fun bboxToH3Indexes(bbox: BBox, resolution: Int): List<String> {
        val polygon = listOf(
            LatLng(bbox.swLat, bbox.swLng), // SW
            LatLng(bbox.neLat, bbox.swLng), // NW
            LatLng(bbox.neLat, bbox.neLng), // NE
            LatLng(bbox.swLat, bbox.neLng)  // SE
        )
        return h3.polygonToCells(polygon, emptyList(), resolution).map { h3.h3ToString(it) }
    }

    /**
     * Redis 캐시 조회 + DB fallback
     */
    private fun getCellsWithCache(
        h3Indexes: List<String>,
        cachePrefix: String,
        dbFetcher: (List<String>) -> List<H3CellData>
    ): List<H3CellData> {
        val result = mutableListOf<H3CellData>()
        val missingIndexes = mutableListOf<String>()

        // Redis multiGet
        val keys = h3Indexes.map { "$cachePrefix$it" }
        val cached = redisTemplate.opsForValue().multiGet(keys) ?: emptyList()

        h3Indexes.forEachIndexed { idx, h3Index ->
            val data = cached.getOrNull(idx)
            if (data != null && data.isNotEmpty()) {
                result.add(deserialize(h3Index, data))
            } else {
                missingIndexes.add(h3Index)
            }
        }

        // Cache miss → DB 조회 후 캐시 저장
        if (missingIndexes.isNotEmpty()) {
            val fromDb = dbFetcher(missingIndexes)
            fromDb.forEach { cell ->
                result.add(cell)
                redisTemplate.opsForValue().set("$cachePrefix${cell.h3Index}", serialize(cell))
            }
            log.debug("[H3Jvm] cache miss=${missingIndexes.size}, db fetched=${fromDb.size}")
        }

        return result
    }

    /**
     * bbox 내 셀만 필터링
     */
    private fun filterByBbox(cells: List<H3CellData>, bbox: BBox): List<H3CellData> {
        return cells.filter { cell ->
            if (cell.cnt == 0) return@filter false
            val lat = cell.sumLat / cell.cnt
            val lng = cell.sumLng / cell.cnt
            lng >= bbox.swLng && lng <= bbox.neLng && lat >= bbox.swLat && lat <= bbox.neLat
        }
    }

    /**
     * 셀 목록 → 집계 결과
     */
    private fun aggregateCells(cells: List<H3CellData>): List<H3JvmCell> {
        return cells.map { cell ->
            H3JvmCell(
                h3Index = cell.h3Index,
                cnt = cell.cnt,
                lat = cell.sumLat / cell.cnt,
                lng = cell.sumLng / cell.cnt
            )
        }
    }

    private fun serialize(cell: H3CellData): ByteArray {
        val buffer = ByteBuffer.allocate(20) // int(4) + double(8) + double(8)
        buffer.putInt(cell.cnt)
        buffer.putDouble(cell.sumLat)
        buffer.putDouble(cell.sumLng)
        return buffer.array()
    }

    private fun deserialize(h3Index: String, data: ByteArray): H3CellData {
        val buffer = ByteBuffer.wrap(data)
        return H3CellData(
            h3Index = h3Index,
            cnt = buffer.getInt(),
            sumLat = buffer.getDouble(),
            sumLng = buffer.getDouble()
        )
    }
}

data class H3JvmCell(
    val h3Index: String,
    val cnt: Int,
    val lat: Double,
    val lng: Double
)

data class H3JvmResponse(
    val cells: List<H3JvmCell>,
    val totalCount: Int,
    val elapsedMs: Long
)

data class H3RegionCell(
    val bjdongCd: String,
    val cnt: Int,
    val lat: Double,
    val lng: Double
)

data class H3RegionResponse(
    val regions: List<H3RegionCell>,
    val totalCount: Int,
    val elapsedMs: Long
)

data class BjdongCellData(
    val bjdongCd: Int,
    val cnt: Short,
    val sumLat: Double,
    val sumLng: Double
)

data class H3FixedGridCell(
    val row: Int,
    val col: Int,
    val cnt: Int,
    val lat: Double,
    val lng: Double,
    val gridSwLat: Double,
    val gridSwLng: Double,
    val gridNeLat: Double,
    val gridNeLng: Double
)

data class H3FixedGridResponse(
    val cells: List<H3FixedGridCell>,
    val totalCount: Int,
    val elapsedMs: Long,
    val error: String?
)

data class H3ViewportGridCell(
    val row: Int,
    val col: Int,
    val cnt: Int,
    val lat: Double,
    val lng: Double
)

data class H3ViewportGridResponse(
    val cells: List<H3ViewportGridCell>,
    val totalCount: Int,
    val elapsedMs: Long,
    val cols: Int,
    val rows: Int,
    val error: String?
)
