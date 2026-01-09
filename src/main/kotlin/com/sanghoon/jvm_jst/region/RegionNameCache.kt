package com.sanghoon.jvm_jst.region

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class RegionNameCache(
    private val repository: BoundaryRegionRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val cache = ConcurrentHashMap<String, String>()

    /**
     * region code -> region name 조회 (캐시 우선)
     */
    fun getNames(regionCodes: Collection<String>): Map<String, String> {
        if (regionCodes.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, String>()
        val missingCodes = mutableListOf<String>()

        // 1. 캐시에서 조회
        for (code in regionCodes) {
            val cached = cache[code]
            if (cached != null) {
                result[code] = cached
            } else {
                missingCodes.add(code)
            }
        }

        // 2. 캐시 미스 → DB 조회 후 캐시 저장
        if (missingCodes.isNotEmpty()) {
            val fromDb = repository.findByRegionCodeIn(missingCodes)
            for (entity in fromDb) {
                val name = entity.regionKoreanName ?: ""
                cache[entity.regionCode] = name
                result[entity.regionCode] = name
            }
            log.debug("[RegionNameCache] hit=${regionCodes.size - missingCodes.size}, miss=${missingCodes.size}, dbFetched=${fromDb.size}")
        }

        return result
    }

    /**
     * 단일 region code 조회
     */
    fun getName(regionCode: String): String? {
        return cache[regionCode] ?: run {
            val entity = repository.findByRegionCodeIn(listOf(regionCode)).firstOrNull()
            entity?.regionKoreanName?.also { cache[regionCode] = it }
        }
    }

    fun cacheSize(): Int = cache.size
}
