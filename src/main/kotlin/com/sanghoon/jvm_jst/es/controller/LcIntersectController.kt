package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.service.LcIntersectIndexingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LC Intersect 인덱싱 컨트롤러
 * geometry를 geo_shape로 저장 (intersects 쿼리용)
 */
@RestController
@RequestMapping("/api/es/lc-intersect")
class LcIntersectController(
    private val indexingService: LcIntersectIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 전체 재인덱싱
     * PUT /api/es/lc-intersect/reindex
     */
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[LC-INTERSECT] reindex 요청")
        val result = indexingService.reindex()
        return ResponseEntity.ok(result)
    }

    /**
     * 인덱스 카운트
     * GET /api/es/lc-intersect/count
     */
    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        val total = indexingService.count()
        return ResponseEntity.ok(mapOf("total" to total))
    }

    /**
     * 인덱스 삭제
     * DELETE /api/es/lc-intersect
     */
    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        log.info("[LC-INTERSECT] deleteIndex 요청")
        val result = indexingService.deleteIndex()
        return ResponseEntity.ok(result)
    }
}
