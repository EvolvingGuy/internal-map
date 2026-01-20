package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.LdrcIndexingService
import com.datahub.geo_poc.es.service.LdrcQueryService
import com.datahub.geo_poc.es.model.LdrcResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/es/ldrc")
class LdrcRestController(
    private val indexingService: LdrcIndexingService,
    private val queryService: LdrcQueryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * LDRC 전체 인덱싱
     * PUT /api/es/ldrc/reindex
     */
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex 요청")
        val result = indexingService.reindex()
        return ResponseEntity.ok(result)
    }

    /**
     * EMD 인덱싱 (인덱스 생성 + PostgreSQL → ES)
     * PUT /api/es/ldrc/reindex/emd
     */
    @PutMapping("/reindex/emd")
    fun reindexEmd(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex EMD 요청")
        val result = indexingService.reindexEmd()
        return ResponseEntity.ok(result)
    }

    /**
     * SGG 인덱싱 (ES EMD → 집계 → ES SGG)
     * PUT /api/es/ldrc/reindex/sgg
     */
    @PutMapping("/reindex/sgg")
    fun reindexSgg(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex SGG 요청")
        val result = indexingService.reindexSgg()
        return ResponseEntity.ok(result)
    }

    /**
     * SD 인덱싱 (ES SGG → 집계 → ES SD)
     * PUT /api/es/ldrc/reindex/sd
     */
    @PutMapping("/reindex/sd")
    fun reindexSd(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex SD 요청")
        val result = indexingService.reindexSd()
        return ResponseEntity.ok(result)
    }

    /**
     * Forcemerge 실행
     * PUT /api/es/ldrc/forcemerge
     */
    @PutMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] forcemerge 요청")
        val result = indexingService.forcemerge()
        return ResponseEntity.ok(result)
    }

    companion object {
        // 레벨별 최소 줌 레벨 (네이버 맵 눈금 기준)
        // SD: 제한없음, SGG: 10(10km), EMD: 14(500m)
        val MIN_ZOOM = mapOf(
            "SD" to 0,
            "SGG" to 10,
            "EMD" to 14
        )

        // 줌 레벨 → 스케일 매핑 (로그용)
        val ZOOM_SCALE = mapOf(
            6 to "100km", 7 to "50km", 8 to "30km", 9 to "20km", 10 to "10km",
            11 to "5km", 12 to "2km", 13 to "1km", 14 to "500m", 15 to "300m",
            16 to "100m", 17 to "50m", 18 to "30m", 19 to "20m", 20 to "10m", 21 to "5m"
        )
    }

    /**
     * LDRC 조회 (무필터용)
     * GET /api/es/ldrc/clusters?swLng=...&swLat=...&neLng=...&neLat=...&level=SD&zoom=10
     */
    @GetMapping("/clusters")
    fun getClusters(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        @RequestParam(defaultValue = "SD") level: String,
        @RequestParam(defaultValue = "10") zoom: Int
    ): ResponseEntity<LdrcResponse> {
        val upperLevel = level.uppercase()
        val minZoom = MIN_ZOOM[upperLevel] ?: 0

        // 줌 레벨 제한 체크
        if (zoom < minZoom) {
            val currentScale = ZOOM_SCALE[zoom] ?: "?"
            val minScale = ZOOM_SCALE[minZoom] ?: "?"
            log.info("[LDRC] 줌 제한: 현재 zoom={} ({}), 최소 zoom={} ({}) 필요. level={}", zoom, currentScale, minZoom, minScale, upperLevel)
            return ResponseEntity.ok(LdrcResponse(
                level = upperLevel,
                h3Count = 0,
                clusters = emptyList(),
                totalCount = 0,
                elapsedMs = 0
            ))
        }

        val scale = ZOOM_SCALE[zoom] ?: "?"
        log.info("[LDRC] 조회: level={}, zoom={} ({})", upperLevel, zoom, scale)
        val response = queryService.queryByBBox(swLng, swLat, neLng, neLat, upperLevel)
        return ResponseEntity.ok(response)
    }

    /**
     * 인덱스 카운트
     * GET /api/es/ldrc/count
     */
    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        val total = indexingService.count()
        return ResponseEntity.ok(mapOf("total" to total))
    }
}
