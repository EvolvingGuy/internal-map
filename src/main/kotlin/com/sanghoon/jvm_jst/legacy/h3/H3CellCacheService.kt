package com.sanghoon.jvm_jst.legacy.h3

import com.uber.h3core.H3Core
import com.uber.h3core.util.LatLng
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.time.Duration

// @Service  // legacy - disabled
// 
class H3CellCacheService(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
    private val emdRepository: H3AggEmdRepository,
    private val sggRepository: H3AggSggRepository,
    private val sdRepository: H3AggSdRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val h3 = H3Core.newInstance()
    private val ttl = Duration.ofDays(7)

    companion object {
        const val RES_EMD = 10
        const val RES_SGG = 9
        const val RES_SD = 6
    }

    fun getEmdCellsByBbox(bbox: BBox): List<H3CellData> {
        return getCellsByBbox(
            bbox = bbox,
            resolution = RES_EMD,
            keyPrefix = "h3cell:emd:",
            fetchFromDb = { missedCells -> emdRepository.findByH3Indexes(missedCells) }
        )
    }

    fun getSggCellsByBbox(bbox: BBox): List<H3CellData> {
        return getCellsByBbox(
            bbox = bbox,
            resolution = RES_SGG,
            keyPrefix = "h3cell:sgg:",
            fetchFromDb = { missedCells -> sggRepository.findByH3Indexes(missedCells) }
        )
    }

    fun getSdCellsByBbox(bbox: BBox): List<H3CellData> {
        return getCellsByBbox(
            bbox = bbox,
            resolution = RES_SD,
            keyPrefix = "h3cell:sd:",
            fetchFromDb = { missedCells -> sdRepository.findByH3Indexes(missedCells) }
        )
    }

    private fun getCellsByBbox(
        bbox: BBox,
        resolution: Int,
        keyPrefix: String,
        fetchFromDb: (Collection<String>) -> List<Array<Any>>
    ): List<H3CellData> {
        val startTime = System.currentTimeMillis()

        // 1. bbox → H3 셀 목록
        val h3Start = System.currentTimeMillis()
        val h3Cells = bboxToH3Cells(bbox, resolution)
        val h3Time = System.currentTimeMillis() - h3Start

        if (h3Cells.isEmpty()) {
            return emptyList()
        }

        val keys = h3Cells.map { keyPrefix + it }
        val result = mutableListOf<H3CellData>()

        // 2. Redis multiGet
        val redisStart = System.currentTimeMillis()
        val cachedValues = redisTemplate.opsForValue().multiGet(keys) ?: List(keys.size) { null }
        val redisTime = System.currentTimeMillis() - redisStart

        val missedCells = mutableListOf<String>()
        var redisHit = 0

        val parseStart = System.currentTimeMillis()
        h3Cells.forEachIndexed { index, cellId ->
            val cached = cachedValues[index]
            if (cached != null && cached.isNotEmpty()) {
                val data = deserialize(cached)
                if (data != null && data.cnt > 0) {
                    result.add(data)
                }
                redisHit++
            } else {
                missedCells.add(cellId)
            }
        }
        val parseTime = System.currentTimeMillis() - parseStart

        // 3. 캐시 미스 → DB 조회
        if (missedCells.isNotEmpty()) {
            val dbStart = System.currentTimeMillis()
            val dbRows = fetchFromDb(missedCells)
            val dbTime = System.currentTimeMillis() - dbStart

            val dbCells = dbRows.map { row ->
                H3CellData(
                    h3Index = row[0] as String,
                    cnt = (row[1] as Number).toInt(),
                    sumLat = (row[2] as Number).toDouble(),
                    sumLng = (row[3] as Number).toDouble()
                )
            }

            result.addAll(dbCells.filter { it.cnt > 0 })

            // 4. Redis에 저장
            val redisSaveStart = System.currentTimeMillis()
            val foundIndexes = dbCells.map { it.h3Index }.toSet()
            redisTemplate.executePipelined { connection ->
                dbCells.forEach { cell ->
                    val key = keyPrefix + cell.h3Index
                    connection.stringCommands().setEx(key.toByteArray(), ttl.seconds, serialize(cell))
                }
                missedCells.filter { it !in foundIndexes }.forEach { cellId ->
                    val key = keyPrefix + cellId
                    val empty = H3CellData(cellId, 0, 0.0, 0.0)
                    connection.stringCommands().setEx(key.toByteArray(), ttl.seconds, serialize(empty))
                }
                null
            }
            val redisSaveTime = System.currentTimeMillis() - redisSaveStart

            log.info("[H3Cell MISS] res=$resolution, missed=${missedCells.size}, found=${dbCells.size} | DB=${dbTime}ms, Save=${redisSaveTime}ms")
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[H3Cell] res=$resolution, cells=${h3Cells.size}, hit=$redisHit, result=${result.size} | H3=${h3Time}ms, Redis=${redisTime}ms, Parse=${parseTime}ms, Total=${elapsed}ms")

        return result
    }

    private fun bboxToH3Cells(bbox: BBox, resolution: Int): List<String> {
        val polygon = listOf(
            LatLng(bbox.swLat, bbox.swLng),
            LatLng(bbox.swLat, bbox.neLng),
            LatLng(bbox.neLat, bbox.neLng),
            LatLng(bbox.neLat, bbox.swLng),
            LatLng(bbox.swLat, bbox.swLng)
        )
        return h3.polygonToCells(polygon, emptyList(), resolution).map { h3.h3ToString(it) }
    }

    // Binary: h3Index(15) + cnt(4) + sumLat(8) + sumLng(8) = 35 bytes
    private fun serialize(data: H3CellData): ByteArray {
        val buffer = ByteBuffer.allocate(35)
        val h3Bytes = data.h3Index.toByteArray(Charsets.US_ASCII)
        buffer.put(h3Bytes, 0, minOf(15, h3Bytes.size))
        repeat(15 - h3Bytes.size) { buffer.put(0) }
        buffer.putInt(data.cnt)
        buffer.putDouble(data.sumLat)
        buffer.putDouble(data.sumLng)
        return buffer.array()
    }

    private fun deserialize(data: ByteArray): H3CellData? {
        if (data.size < 35) return null
        val buffer = ByteBuffer.wrap(data)
        val h3Bytes = ByteArray(15)
        buffer.get(h3Bytes)
        val h3Index = String(h3Bytes, Charsets.US_ASCII).trimEnd('\u0000')
        val cnt = buffer.int
        val sumLat = buffer.double
        val sumLng = buffer.double
        return H3CellData(h3Index, cnt, sumLat, sumLng)
    }
}

data class H3CellData(
    val h3Index: String,
    val cnt: Int,
    val sumLat: Double,
    val sumLng: Double
) {
    fun avgLat() = if (cnt > 0) sumLat / cnt else 0.0
    fun avgLng() = if (cnt > 0) sumLng / cnt else 0.0
}
