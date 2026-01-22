package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.service.LcGeoIndexingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LC Geo 인덱싱 컨트롤러
 * geometry를 object로 저장하는 버전
 */
@RestController
@RequestMapping("/api/es/lc-geo")
class LcGeoController(
    private val indexingService: LcGeoIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 전체 재인덱싱
     * PUT /api/es/lc-geo/reindex
     */
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[LC-GEO] reindex 요청")
        val result = indexingService.reindex()
        return ResponseEntity.ok(result)
    }

    /**
     * 인덱스 카운트
     * GET /api/es/lc-geo/count
     */
    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        val total = indexingService.count()
        return ResponseEntity.ok(mapOf("total" to total))
    }

    /**
     * 인덱스 삭제
     * DELETE /api/es/lc-geo
     */
    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        log.info("[LC-GEO] deleteIndex 요청")
        val result = indexingService.deleteIndex()
        return ResponseEntity.ok(result)
    }
}
