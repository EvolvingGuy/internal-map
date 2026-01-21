package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.service.RegistrationIndexingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Registration ES 인덱싱 API Controller
 */
@RestController
@RequestMapping("/api/es/registration")
class RegistrationIndexingController(
    private val indexingService: RegistrationIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 전체 재인덱싱 (동기)
     * PUT /api/es/registration/reindex
     */
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[Registration] reindex 요청")
        val result = indexingService.reindex()
        return ResponseEntity.ok(result)
    }

    /**
     * 인덱스 카운트
     * GET /api/es/registration/count
     */
    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Any>> {
        val cnt = indexingService.count()
        return ResponseEntity.ok(mapOf("index" to "registration", "count" to cnt))
    }

    /**
     * forcemerge 실행
     * POST /api/es/registration/forcemerge
     */
    @PostMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        log.info("[Registration] forcemerge 요청")
        val result = indexingService.forcemerge()
        return ResponseEntity.ok(result)
    }

    /**
     * 인덱스 삭제
     * DELETE /api/es/registration
     */
    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        log.info("[Registration] 인덱스 삭제 요청")
        val result = indexingService.deleteIndex()
        return ResponseEntity.ok(result)
    }
}
