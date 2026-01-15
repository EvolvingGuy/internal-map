package com.sanghoon.jvm_jst.legacy.h3

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.time.Duration

// @Service  // legacy - disabled
// 
class H3CacheService(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
    private val emdRepository: H3AggEmdRepository,
    private val sggRepository: H3AggSggRepository,
    private val sdRepository: H3AggSdRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val ttl = Duration.ofDays(7)

    // ========== EMD10 (res 10) ==========
    fun getEmdByRegionCodes(regionCodes: Collection<String>): Map<String, List<H3CellCacheDto>> {
        return getByCodesGeneric(
            codes = regionCodes,
            keyPrefix = "h3:emd:",
            fetchFromDb = { missedCodes -> emdRepository.findByRegionCodes(missedCodes) },
            groupByIndex = 4,
            label = "Emd"
        )
    }

    // ========== SGG (res 9) ==========
    fun getSggByCodes(sggCodes: Collection<String>): Map<String, List<H3CellCacheDto>> {
        return getByCodesGeneric(
            codes = sggCodes,
            keyPrefix = "h3:sgg:",
            fetchFromDb = { missedCodes -> sggRepository.findBySggCodes(missedCodes) },
            groupByIndex = 4,
            label = "Sgg"
        )
    }

    // ========== SD (res 6) ==========
    fun getSdByCodes(sdCodes: Collection<String>): Map<String, List<H3CellCacheDto>> {
        return getByCodesGeneric(
            codes = sdCodes,
            keyPrefix = "h3:sd:",
            fetchFromDb = { missedCodes -> sdRepository.findBySdCodes(missedCodes) },
            groupByIndex = 4,
            label = "Sd"
        )
    }

    // ========== Generic Cache Logic ==========
    private fun getByCodesGeneric(
        codes: Collection<String>,
        keyPrefix: String,
        fetchFromDb: (Collection<String>) -> List<Array<Any>>,
        groupByIndex: Int,
        label: String
    ): Map<String, List<H3CellCacheDto>> {
        if (codes.isEmpty()) return emptyMap()

        val codeList = codes.toList()
        val keys = codeList.map { keyPrefix + it }
        val result = mutableMapOf<String, List<H3CellCacheDto>>()

        // 1. Redis multiGet
        val redisStart = System.currentTimeMillis()
        val cachedValues = redisTemplate.opsForValue().multiGet(keys) ?: List(keys.size) { null }
        val redisTime = System.currentTimeMillis() - redisStart

        val missedCodes = mutableListOf<String>()
        var redisHit = 0
        var totalBytes = 0L

        val parseStart = System.currentTimeMillis()
        codeList.forEachIndexed { index, code ->
            val cached = cachedValues[index]
            if (cached != null) {
                totalBytes += cached.size
                result[code] = deserialize(cached)
                redisHit++
            } else {
                missedCodes.add(code)
            }
        }
        val parseTime = System.currentTimeMillis() - parseStart

        log.info("[H3Cache $label] keys=${keys.size}, hit=$redisHit, miss=${missedCodes.size}, bytes=${totalBytes/1024}KB | Redis=${redisTime}ms, Parse=${parseTime}ms")

        // 2. DB에서 미스된 것들 조회
        if (missedCodes.isNotEmpty()) {
            val dbStart = System.currentTimeMillis()
            val rows = fetchFromDb(missedCodes)
            val dbTime = System.currentTimeMillis() - dbStart

            val grouped = rows.groupBy { it[groupByIndex] as String }
            val toCache = mutableMapOf<String, ByteArray>()

            for ((code, rowList) in grouped) {
                val dtos = rowList.map { row -> rowToDto(row) }
                result[code] = dtos
                toCache[keyPrefix + code] = serialize(dtos)
            }

            // 빈 결과도 캐시
            for (code in missedCodes) {
                if (code !in result) {
                    result[code] = emptyList()
                    toCache[keyPrefix + code] = serialize(emptyList())
                }
            }

            // Redis pipeline 저장
            val redisSaveStart = System.currentTimeMillis()
            redisTemplate.executePipelined { connection ->
                toCache.forEach { (key, value) ->
                    connection.stringCommands().setEx(key.toByteArray(), ttl.seconds, value)
                }
                null
            }
            val redisSaveTime = System.currentTimeMillis() - redisSaveStart

            log.info("[H3Cache $label MISS] codes=${missedCodes.size}, rows=${rows.size} | DB=${dbTime}ms, RedisSave=${redisSaveTime}ms")
        }

        return result
    }

    // Binary 직렬화: [4 bytes count][반복: 15 bytes h3Index + 4 bytes cnt + 8 bytes sumLat + 8 bytes sumLng]
    private fun serialize(dtos: List<H3CellCacheDto>): ByteArray {
        val buffer = ByteBuffer.allocate(4 + dtos.size * 35)
        buffer.putInt(dtos.size)
        for (dto in dtos) {
            val h3Bytes = dto.h3Index.toByteArray(Charsets.US_ASCII)
            buffer.put(h3Bytes, 0, minOf(15, h3Bytes.size))
            repeat(15 - h3Bytes.size) { buffer.put(0) }
            buffer.putInt(dto.cnt)
            buffer.putDouble(dto.sumLat)
            buffer.putDouble(dto.sumLng)
        }
        return buffer.array()
    }

    private fun deserialize(data: ByteArray): List<H3CellCacheDto> {
        val buffer = ByteBuffer.wrap(data)
        val count = buffer.int
        val result = ArrayList<H3CellCacheDto>(count)
        val h3Bytes = ByteArray(15)
        repeat(count) {
            buffer.get(h3Bytes)
            val h3Index = String(h3Bytes, Charsets.US_ASCII).trimEnd('\u0000')
            val cnt = buffer.int
            val sumLat = buffer.double
            val sumLng = buffer.double
            result.add(H3CellCacheDto(h3Index, cnt, sumLat, sumLng))
        }
        return result
    }

    private fun rowToDto(row: Array<Any>): H3CellCacheDto {
        return H3CellCacheDto(
            h3Index = row[0] as String,
            cnt = (row[1] as Number).toInt(),
            sumLat = (row[2] as Number).toDouble(),
            sumLng = (row[3] as Number).toDouble()
        )
    }
}

data class H3CellCacheDto(
    val h3Index: String,
    val cnt: Int,
    val sumLat: Double,
    val sumLng: Double
)
