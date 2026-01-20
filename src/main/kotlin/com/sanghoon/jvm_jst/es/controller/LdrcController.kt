package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.service.LdrcIndexingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LDRC (Land Dynamic Region Cluster) 인덱싱 컨트롤러
 * 가변형 행정구역 클러스터 (H3 기반)
 */
@RestController
@RequestMapping("/api/es/ldrc")
class LdrcController(
    private val indexingService: LdrcIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 전체 재인덱싱 (EMD -> SGG -> SD)
     * PUT /api/es/ldrc/reindex
     */
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex 요청")
        val result = indexingService.reindex()
        return ResponseEntity.ok(result)
    }

    /**
     * EMD 인덱싱 (인덱스 생성 + PostgreSQL -> ES)
     * PUT /api/es/ldrc/reindex/emd
     */
    @PutMapping("/reindex/emd")
    fun reindexEmd(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex EMD 요청")
        val result = indexingService.reindexEmd()
        return ResponseEntity.ok(result)
    }

    /**
     * SGG 인덱싱 (ES EMD -> 집계 -> ES SGG)
     * PUT /api/es/ldrc/reindex/sgg
     */
    @PutMapping("/reindex/sgg")
    fun reindexSgg(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex SGG 요청")
        val result = indexingService.reindexSgg()
        return ResponseEntity.ok(result)
    }

    /**
     * SD 인덱싱 (ES SGG -> 집계 -> ES SD)
     * PUT /api/es/ldrc/reindex/sd
     */
    @PutMapping("/reindex/sd")
    fun reindexSd(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex SD 요청")
        val result = indexingService.reindexSd()
        return ResponseEntity.ok(result)
    }

    /**
     * forcemerge 실행
     * PUT /api/es/ldrc/forcemerge
     */
    @PutMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] forcemerge 요청")
        val result = indexingService.forcemerge()
        return ResponseEntity.ok(result)
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

    /**
     * 인덱스 삭제
     * DELETE /api/es/ldrc
     */
    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] deleteIndex 요청")
        val result = indexingService.deleteIndex()
        return ResponseEntity.ok(result)
    }
}
