package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lsrc.LsrcIndexingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LSRC (Land Static Region Cluster) 인덱싱 컨트롤러
 * 고정형 행정구역 클러스터
 */
@RestController
@RequestMapping("/api/es/lsrc")
class LsrcRestController(
    private val indexingService: LsrcIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 전체 재인덱싱
     * PUT /api/es/lsrc/reindex
     */
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[LSRC] reindex 요청")
        val result = indexingService.reindex()
        return ResponseEntity.ok(result)
    }

    /**
     * forcemerge 실행
     * PUT /api/es/lsrc/forcemerge
     */
    @PutMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        log.info("[LSRC] forcemerge 요청")
        val result = indexingService.forcemerge()
        return ResponseEntity.ok(result)
    }

    /**
     * 인덱스 카운트
     * GET /api/es/lsrc/count
     */
    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        val total = indexingService.count()
        return ResponseEntity.ok(mapOf("total" to total))
    }

    /**
     * 인덱스 삭제
     * DELETE /api/es/lsrc
     */
    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        log.info("[LSRC] deleteIndex 요청")
        val result = indexingService.deleteIndex()
        return ResponseEntity.ok(result)
    }
}
