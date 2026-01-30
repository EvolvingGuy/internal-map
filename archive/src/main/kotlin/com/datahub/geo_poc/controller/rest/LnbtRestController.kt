package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbt.LnbtIndexingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

/**
 * LNBT (Land Nested Building Trade) 인덱싱 컨트롤러
 * 건물과 실거래 모두 nested array로 저장
 */
@RestController
@RequestMapping("/api/es/lnbt")
class LnbtRestController(
    private val indexingService: LnbtIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 전체 재인덱싱 (백그라운드 실행)
     * PUT /api/es/lnbt/reindex
     */
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[LNBT] reindex 요청 - 백그라운드 실행 시작")
        CompletableFuture.runAsync {
            try {
                indexingService.reindex()
            } catch (e: Exception) {
                log.error("[LNBT] reindex 실패", e)
            }
        }
        return ResponseEntity.accepted().body(mapOf(
            "status" to "started",
            "message" to "Reindex started in background. Check /api/es/lnbt/count for progress."
        ))
    }

    /**
     * forcemerge 실행
     * PUT /api/es/lnbt/forcemerge
     */
    @PutMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        log.info("[LNBT] forcemerge 요청")
        val result = indexingService.forcemerge()
        return ResponseEntity.ok(result)
    }

    /**
     * 인덱스 카운트
     * GET /api/es/lnbt/count
     */
    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        val total = indexingService.count()
        return ResponseEntity.ok(mapOf("total" to total))
    }

    /**
     * 인덱스 삭제
     * DELETE /api/es/lnbt
     */
    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        log.info("[LNBT] deleteIndex 요청")
        val result = indexingService.deleteIndex()
        return ResponseEntity.ok(result)
    }
}
