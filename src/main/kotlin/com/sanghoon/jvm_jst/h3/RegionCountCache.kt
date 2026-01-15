package com.sanghoon.jvm_jst.h3

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class RegionCountCache(
    private val repository: RegionCountRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // regionCode -> RegionCountCacheData
    private val emdCache = ConcurrentHashMap<String, RegionCountCacheData>()
    private val sggCache = ConcurrentHashMap<String, RegionCountCacheData>()
    private val sdCache = ConcurrentHashMap<String, RegionCountCacheData>()

    @Volatile
    private var loaded = false

    fun init() {
        Thread {
            log.info("RegionCountCache 프리로드 시작")
            val startTime = System.currentTimeMillis()

            try {
                val allData = repository.findAll()

                for (entity in allData) {
                    val cacheData = RegionCountCacheData(
                        cnt = entity.cnt,
                        centerLat = entity.centerLat,
                        centerLng = entity.centerLng
                    )

                    when (entity.regionLevel) {
                        "emd" -> emdCache[entity.regionCode] = cacheData
                        "sgg" -> sggCache[entity.regionCode] = cacheData
                        "sd" -> sdCache[entity.regionCode] = cacheData
                    }
                }

                loaded = true
                val elapsed = System.currentTimeMillis() - startTime

                // 용량 계산 (엔트리당 약 112 bytes: 키 40 + 값 40 + 맵 오버헤드 32)
                val bytesPerEntry = 112
                val totalEntries = emdCache.size + sggCache.size + sdCache.size
                val estimatedMB = String.format("%.2f", (totalEntries * bytesPerEntry) / (1024.0 * 1024.0))

                log.info("RegionCountCache 프리로드 완료: emd={}개, sgg={}개, sd={}개, 총 {}개, 약 {}MB, {}ms",
                    emdCache.size, sggCache.size, sdCache.size, totalEntries, estimatedMB, elapsed)
            } catch (e: Exception) {
                log.error("RegionCountCache 프리로드 실패", e)
            }
        }.start()
    }

    /**
     * bbox 내 emd 데이터 반환 (좌표 필터링)
     */
    fun getEmdByBbox(bbox: BBox): Map<String, RegionCountCacheData> {
        return emdCache.filter { (_, data) ->
            data.centerLat >= bbox.swLat && data.centerLat <= bbox.neLat &&
            data.centerLng >= bbox.swLng && data.centerLng <= bbox.neLng
        }
    }

    /**
     * bbox 내 sgg 데이터 반환 (좌표 필터링)
     */
    fun getSggByBbox(bbox: BBox): Map<String, RegionCountCacheData> {
        return sggCache.filter { (_, data) ->
            data.centerLat >= bbox.swLat && data.centerLat <= bbox.neLat &&
            data.centerLng >= bbox.swLng && data.centerLng <= bbox.neLng
        }
    }

    /**
     * bbox 내 sd 데이터 반환 (좌표 필터링)
     */
    fun getSdByBbox(bbox: BBox): Map<String, RegionCountCacheData> {
        return sdCache.filter { (_, data) ->
            data.centerLat >= bbox.swLat && data.centerLat <= bbox.neLat &&
            data.centerLng >= bbox.swLng && data.centerLng <= bbox.neLng
        }
    }

    fun isLoaded(): Boolean = loaded
    fun getCacheSize(): Triple<Int, Int, Int> = Triple(emdCache.size, sggCache.size, sdCache.size)
}

data class RegionCountCacheData(
    val cnt: Int,
    val centerLat: Double,
    val centerLng: Double
)
