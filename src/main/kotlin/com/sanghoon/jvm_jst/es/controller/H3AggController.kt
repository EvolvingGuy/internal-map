package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.service.H3AggIndexingService
import com.sanghoon.jvm_jst.es.service.H3AggQueryService
import com.sanghoon.jvm_jst.es.service.H3AggResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/es/h3-agg")
class H3AggController(
    private val indexingService: H3AggIndexingService,
    private val queryService: H3AggQueryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * H3 집계 전체 인덱싱
     * PUT /api/es/h3-agg/reindex
     */
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[H3Agg] reindex 요청")
        val result = indexingService.reindex()
        return ResponseEntity.ok(result)
    }

    /**
     * EMD 인덱싱 (인덱스 생성 + PostgreSQL → ES)
     * PUT /api/es/h3-agg/reindex/emd
     */
    @PutMapping("/reindex/emd")
    fun reindexEmd(): ResponseEntity<Map<String, Any>> {
        log.info("[H3Agg] reindex EMD 요청")
        val result = indexingService.reindexEmd()
        return ResponseEntity.ok(result)
    }

    /**
     * SGG 인덱싱 (ES EMD → 집계 → ES SGG)
     * PUT /api/es/h3-agg/reindex/sgg
     */
    @PutMapping("/reindex/sgg")
    fun reindexSgg(): ResponseEntity<Map<String, Any>> {
        log.info("[H3Agg] reindex SGG 요청")
        val result = indexingService.reindexSgg()
        return ResponseEntity.ok(result)
    }

    /**
     * SD 인덱싱 (ES SGG → 집계 → ES SD)
     * PUT /api/es/h3-agg/reindex/sd
     */
    @PutMapping("/reindex/sd")
    fun reindexSd(): ResponseEntity<Map<String, Any>> {
        log.info("[H3Agg] reindex SD 요청")
        val result = indexingService.reindexSd()
        return ResponseEntity.ok(result)
    }

    /**
     * Forcemerge 실행
     * PUT /api/es/h3-agg/forcemerge
     */
    @PutMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        log.info("[H3Agg] forcemerge 요청")
        val result = indexingService.forcemerge()
        return ResponseEntity.ok(result)
    }

    /**
     * H3 집계 조회 (무필터용)
     * GET /api/es/h3-agg/clusters?swLng=...&swLat=...&neLng=...&neLat=...&level=SD
     */
    @GetMapping("/clusters")
    fun getClusters(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        @RequestParam(defaultValue = "SD") level: String
    ): ResponseEntity<H3AggResponse> {
        log.info("[H3Agg] query level={}, bbox=({},{})~({},{})", level, swLat, swLng, neLat, neLng)
        val response = queryService.queryByBBox(swLng, swLat, neLng, neLat, level.uppercase())
        return ResponseEntity.ok(response)
    }

    /**
     * 인덱스 카운트
     * GET /api/es/h3-agg/count
     */
    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        val total = indexingService.count()
        return ResponseEntity.ok(mapOf("total" to total))
    }
}
