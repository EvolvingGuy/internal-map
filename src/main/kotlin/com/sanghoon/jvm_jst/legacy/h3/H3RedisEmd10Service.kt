package com.sanghoon.jvm_jst.legacy.h3

import com.sanghoon.jvm_jst.legacy.h3.proto.H3Emd10Proto.BjdongCellDataProto
import com.sanghoon.jvm_jst.legacy.h3.proto.H3Emd10Proto.BjdongCellListProto
import com.uber.h3core.H3Core
import com.uber.h3core.util.LatLng
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

// @Service  // legacy - disabled
// 
class H3RedisEmd10Service(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
    private val repository: H3AggEmdRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val h3 = H3Core.newInstance()

    companion object {
        private const val CACHE_PREFIX = "h3:emd10:proto:"
        private const val RESOLUTION = 10
    }

    /**
     * 읍면동별 집계 (Redis 캐시)
     */
    fun getRegionAggregation(bbox: BBox): H3RegionResponse {
        val startTime = System.currentTimeMillis()

        // 1. bbox → H3 인덱스
        val h3Start = System.currentTimeMillis()
        val h3Indexes = bboxToH3Indexes(bbox)
        val h3Time = System.currentTimeMillis() - h3Start

        if (h3Indexes.isEmpty()) {
            return H3RegionResponse(emptyList(), 0, 0)
        }

        // 2. Redis 조회 (캐시 미스는 DB 조회 후 캐시)
        val cacheStart = System.currentTimeMillis()
        val cachedData = getFromRedisOrDb(h3Indexes)
        val cacheTime = System.currentTimeMillis() - cacheStart

        // 3. bjdong_cd로 그룹핑 및 집계
        val aggStart = System.currentTimeMillis()
        val grouped = mutableMapOf<Int, BjdongAggData>()
        var fetchedRecords = 0
        var fetchedBytes = 0L

        for ((_, cellList) in cachedData) {
            fetchedRecords += cellList.cellsCount
            for (cell in cellList.cellsList) {
                val existing = grouped[cell.bjdongCd]
                if (existing != null) {
                    existing.cnt += cell.cnt
                    existing.sumLat += cell.sumLat
                    existing.sumLng += cell.sumLng
                } else {
                    grouped[cell.bjdongCd] = BjdongAggData(cell.cnt, cell.sumLat, cell.sumLng)
                }
            }
        }

        // 4. 결과 변환
        val result = grouped.map { (bjdongCd, data) ->
            H3RegionCell(
                bjdongCd = bjdongCd.toString(),
                cnt = data.cnt,
                lat = data.sumLat / data.cnt,
                lng = data.sumLng / data.cnt
            )
        }
        val aggTime = System.currentTimeMillis() - aggStart

        val elapsed = System.currentTimeMillis() - startTime
        val fetchedKB = fetchedBytes / 1024.0
        log.info(
            "[H3 Emd10 Redis] h3={}ms → cache={}ms → agg={}ms | indexes={}, fetched={} records, total={}ms",
            h3Time, cacheTime, aggTime, h3Indexes.size, fetchedRecords, elapsed
        )

        return H3RegionResponse(result, result.sumOf { it.cnt }, elapsed)
    }

    /**
     * Redis에서 조회, 없으면 DB 조회 후 Redis 캐시
     */
    private fun getFromRedisOrDb(h3Indexes: List<String>): Map<String, BjdongCellListProto> {
        val result = mutableMapOf<String, BjdongCellListProto>()
        val missingIndexes = mutableListOf<String>()

        // Redis mget 조회
        val redisStart = System.currentTimeMillis()
        val keys = h3Indexes.map { "$CACHE_PREFIX$it" }
        val values = redisTemplate.opsForValue().multiGet(keys)

        for (i in h3Indexes.indices) {
            val h3Index = h3Indexes[i]
            val cached = values?.get(i)
            if (cached != null) {
                result[h3Index] = BjdongCellListProto.parseFrom(cached)
            } else {
                missingIndexes.add(h3Index)
            }
        }
        val redisTime = System.currentTimeMillis() - redisStart

        if (missingIndexes.isEmpty()) {
            log.debug("[Redis] 전체 캐시 히트: {} indexes, {}ms", h3Indexes.size, redisTime)
            return result
        }

        // DB 조회
        val dbStart = System.currentTimeMillis()
        val rows = repository.findByH3IndexesWithBjdong(missingIndexes)
        val dbTime = System.currentTimeMillis() - dbStart

        // 그룹핑 및 Redis 저장 (mset)
        val saveStart = System.currentTimeMillis()
        val grouped = rows.groupBy { it[1] as String }
        var savedBytes = 0L
        val toSave = mutableMapOf<String, ByteArray>()

        for (h3Index in missingIndexes) {
            val group = grouped[h3Index]
            val protoBuilder = BjdongCellListProto.newBuilder()

            if (group != null) {
                for (row in group) {
                    protoBuilder.addCells(
                        BjdongCellDataProto.newBuilder()
                            .setBjdongCd((row[0] as Number).toInt())
                            .setCnt((row[2] as Number).toInt())
                            .setSumLat((row[3] as Number).toDouble())
                            .setSumLng((row[4] as Number).toDouble())
                            .build()
                    )
                }
            }

            val proto = protoBuilder.build()
            val protoBytes = proto.toByteArray()
            toSave["$CACHE_PREFIX$h3Index"] = protoBytes
            result[h3Index] = proto
            savedBytes += protoBytes.size
        }

        if (toSave.isNotEmpty()) {
            redisTemplate.opsForValue().multiSet(toSave)
        }
        val saveTime = System.currentTimeMillis() - saveStart

        log.info(
            "[Redis] 캐시 미스: {} / {} | redis={}ms, db={}ms, save={}ms, saved={} KB",
            missingIndexes.size, h3Indexes.size, redisTime, dbTime, saveTime,
            String.format("%.2f", savedBytes / 1024.0)
        )

        return result
    }

    private fun bboxToH3Indexes(bbox: BBox): List<String> {
        val polygon = listOf(
            LatLng(bbox.swLat, bbox.swLng),
            LatLng(bbox.neLat, bbox.swLng),
            LatLng(bbox.neLat, bbox.neLng),
            LatLng(bbox.swLat, bbox.neLng)
        )
        return h3.polygonToCells(polygon, emptyList(), RESOLUTION).map { h3.h3ToString(it) }
    }

    private class BjdongAggData(var cnt: Int, var sumLat: Double, var sumLng: Double)
}
