package com.sanghoon.jvm_jst.rds.legacy

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.time.Duration

@Service
class PnuCacheService(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
    private val pnuRepository: PnuRepository
) {
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)
    private val keyPrefix = "pnu:region:"
    private val ttl = Duration.ofDays(7)
    private val majorCityCodes = PnuCacheWarmupRunner.MAJOR_CITY_CODES

    /**
     * 읍면동리 코드로 PNU 리스트 조회 (캐시 우선)
     */
    fun getByRegionCode(regionCode: String): List<PnuCacheDto> {
        val key = keyPrefix + regionCode

        // 1. Redis 캐시 확인
        val cached = redisTemplate.opsForValue().get(key)
        if (cached != null) {
            return deserialize(cached)
        }

        // 2. DB 조회 (native query)
        val rows = pnuRepository.findByRegionCodeNative(regionCode)

        // 3. 변환 및 캐시 (빈 결과도 캐시)
        val dtos = rows.map { row -> rowToDto(row) }
        val bytes = serialize(dtos)
        if (regionCode.take(2) in majorCityCodes) {
            redisTemplate.opsForValue().set(key, bytes)
        } else {
            redisTemplate.opsForValue().set(key, bytes, ttl)
        }

        return dtos
    }

    /**
     * 여러 읍면동리 코드로 PNU 리스트 조회 (Redis 캐시)
     */
    fun getByRegionCodes(regionCodes: Collection<String>): Map<String, List<PnuCacheDto>> {
        if (regionCodes.isEmpty()) return emptyMap()

        val codeList = regionCodes.toList()
        val keys = codeList.map { keyPrefix + it }
        val result = mutableMapOf<String, List<PnuCacheDto>>()

        // 1. Redis multiGet
        val redisStart = System.currentTimeMillis()
        val cachedValues = redisTemplate.opsForValue().multiGet(keys) ?: List(keys.size) { null }
        val redisTime = System.currentTimeMillis() - redisStart

        val missedCodes = mutableListOf<String>()
        var redisHit = 0
        var totalBytes = 0L

        val parseStart = System.currentTimeMillis()

        // 순차 파싱
        codeList.forEachIndexed { index, code ->
            val cached = cachedValues[index]
            if (cached != null) {
                totalBytes += cached.size
                val dtos = deserialize(cached)
                result[code] = dtos
                redisHit++
            } else {
                missedCodes.add(code)
            }
        }

        val parseTime = System.currentTimeMillis() - parseStart

        log.info("[Cache] keys=${keys.size}, hit=$redisHit, miss=${missedCodes.size}, bytes=${totalBytes/1024}KB | Redis=${redisTime}ms, Parse=${parseTime}ms")

        // 2. 없는 것들 DB IN절로 조회
        if (missedCodes.isNotEmpty()) {
            val dbStart = System.currentTimeMillis()
            val rows = pnuRepository.findByRegionCodesNative(missedCodes)
            val dbTime = System.currentTimeMillis() - dbStart

            val grouped = rows.groupBy { it[1] as String }  // bjdong_cd로 그룹핑

            // 캐시에 저장할 데이터 준비 (pipeline으로 한 번에)
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

            // Redis pipeline으로 한 번에 저장
            val redisSaveStart = System.currentTimeMillis()
            redisTemplate.executePipelined { connection ->
                toCache.forEach { (key, value) ->
                    val regionCode = key.removePrefix(keyPrefix)
                    val sidoCode = regionCode.take(2)
                    if (sidoCode in majorCityCodes) {
                        // 주요 광역시: TTL 없이 영구 저장
                        connection.stringCommands().set(key.toByteArray(), value)
                    } else {
                        // 나머지: TTL 적용
                        connection.stringCommands().setEx(key.toByteArray(), ttl.seconds, value)
                    }
                }
                null
            }
            val redisSaveTime = System.currentTimeMillis() - redisSaveStart

            log.info("[Cache MISS] codes=${missedCodes.size} $missedCodes, rows=${rows.size} | DB=${dbTime}ms, RedisSave=${redisSaveTime}ms")
        }

        return result
    }

    /**
     * 캐시 무효화
     */
    fun invalidate(regionCode: String) {
        redisTemplate.delete(keyPrefix + regionCode)
    }

    fun invalidateAll(regionCodes: Collection<String>) {
        redisTemplate.delete(regionCodes.map { keyPrefix + it })
    }

    // Binary 직렬화: [4 bytes count][반복: 19 bytes PNU + 8 bytes Long]
    private fun serialize(dtos: List<PnuCacheDto>): ByteArray {
        val buffer = ByteBuffer.allocate(4 + dtos.size * 27)
        buffer.putInt(dtos.size)
        for (dto in dtos) {
            val pnuBytes = dto.p.toByteArray(Charsets.US_ASCII)
            buffer.put(pnuBytes, 0, minOf(19, pnuBytes.size))
            repeat(19 - pnuBytes.size) { buffer.put(0) }
            buffer.putLong(dto.c)
        }
        return buffer.array()
    }

    private fun deserialize(data: ByteArray): List<PnuCacheDto> {
        val buffer = ByteBuffer.wrap(data)
        val count = buffer.int
        val result = ArrayList<PnuCacheDto>(count)
        val pnuBytes = ByteArray(19)
        repeat(count) {
            buffer.get(pnuBytes)
            val pnu = String(pnuBytes, Charsets.US_ASCII).trimEnd('\u0000')
            result.add(PnuCacheDto(pnu, buffer.long))
        }
        return result
    }

    private fun rowToDto(row: Array<Any>): PnuCacheDto {
        val pnu = row[0] as String
        val lng = (row[2] as Number).toDouble()
        val lat = (row[3] as Number).toDouble()
        return PnuCacheDto(pnu, CoordinateCodec.encode(lat, lng))
    }
}
