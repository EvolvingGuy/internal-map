package com.sanghoon.jvm_jst.rds.cache

import tools.jackson.databind.ObjectMapper
import com.sanghoon.jvm_jst.rds.entity.PnuBoundaryRegion
import com.sanghoon.jvm_jst.rds.repository.PnuBoundaryRegionRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * 행정구역 경계 캐시 DTO (최소 필드)
 */
data class BoundaryRegionCacheData(
    val code: String,           // region_code
    val name: String?,          // region_korean_name
    val fullName: String?,      // region_full_korean_name
    val geom: String?,          // geom
    val isDonut: Boolean?,      // is_donut_polygon
    val cLng: Double?,          // center_lng
    val cLat: Double?           // center_lat
)

/**
 * 행정구역 경계 캐시 서비스
 */
@Service
class BoundaryRegionCacheService(
    private val redisTemplate: StringRedisTemplate,
    private val repository: PnuBoundaryRegionRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val TTL: Duration = Duration.ofDays(1)
        private const val PREFIX = "boundary_region:"
    }

    /**
     * 단일 region code로 조회
     */
    fun get(regionCode: String): BoundaryRegionCacheData? {
        val key = "$PREFIX$regionCode"
        val cached = redisTemplate.opsForValue().get(key)

        if (cached != null) {
            return if (cached == "null") null else deserialize(cached)
        }

        // DB 조회
        val entity = repository.findByRegionCode(regionCode)
        val data = entity?.toCacheData()

        // 캐시 저장 (없으면 "null" 저장 - negative cache)
        redisTemplate.opsForValue().set(key, data?.let { serialize(it) } ?: "null", TTL)

        return data
    }

    /**
     * 여러 region code로 조회
     */
    fun multiGet(regionCodes: Collection<String>): Map<String, BoundaryRegionCacheData?> {
        if (regionCodes.isEmpty()) return emptyMap()

        val keys = regionCodes.map { "$PREFIX$it" }
        val values = redisTemplate.opsForValue().multiGet(keys) ?: return emptyMap()

        val result = mutableMapOf<String, BoundaryRegionCacheData?>()
        val missingCodes = mutableListOf<String>()

        regionCodes.forEachIndexed { idx, code ->
            val cached = values.getOrNull(idx)
            if (cached != null) {
                result[code] = if (cached == "null") null else deserialize(cached)
            } else {
                missingCodes.add(code)
            }
        }

        // 캐시 미스 → DB 조회
        if (missingCodes.isNotEmpty()) {
            val fromDb = repository.findByRegionCodes(missingCodes).associateBy { it.regionCode }

            for (code in missingCodes) {
                val entity = fromDb[code]
                val data = entity?.toCacheData()
                result[code] = data

                // 캐시 저장
                val key = "$PREFIX$code"
                redisTemplate.opsForValue().set(key, data?.let { serialize(it) } ?: "null", TTL)
            }
        }

        return result
    }

    /**
     * 계층적 조회 (시도 > 시군구 > 읍면동)
     * 8자리 읍면동 코드 → [2자리 시도, 5자리 시군구, 8자리 읍면동]
     */
    fun getHierarchy(emdCode: String): List<BoundaryRegionCacheData> {
        if (emdCode.length < 8) return emptyList()

        val codes = listOf(
            emdCode.substring(0, 2),  // 시도
            emdCode.substring(0, 5),  // 시군구
            emdCode.substring(0, 8)   // 읍면동
        )

        return multiGet(codes).values.filterNotNull().sortedBy { it.code.length }
    }

    private fun serialize(data: BoundaryRegionCacheData): String =
        objectMapper.writeValueAsString(data)

    private fun deserialize(json: String): BoundaryRegionCacheData =
        objectMapper.readValue(json, BoundaryRegionCacheData::class.java)

    private fun PnuBoundaryRegion.toCacheData() = BoundaryRegionCacheData(
        code = regionCode,
        name = regionKoreanName,
        fullName = regionFullKoreanName,
        geom = geom,
        isDonut = isDonutPolygon,
        cLng = centerLng,
        cLat = centerLat
    )
}
