package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnb.LnbIndexingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

/**
 * LNB (Land Nested Building) 인덱싱 컨트롤러
 * 건물을 nested array로 저장
 */
@RestController
@RequestMapping("/api/es/lnb")
class LnbRestController(
    private val indexingService: LnbIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 전체 재인덱싱 (백그라운드 실행)
     * PUT /api/es/lnb/reindex
     */
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[LNB] reindex 요청 - 백그라운드 실행 시작")
        CompletableFuture.runAsync {
            try {
                indexingService.reindex()
            } catch (e: Exception) {
                log.error("[LNB] reindex 실패", e)
            }
        }
        return ResponseEntity.accepted().body(mapOf(
            "status" to "started",
            "message" to "Reindex started in background. Check /api/es/lnb/count for progress."
        ))
    }

    /**
     * forcemerge 실행
     * PUT /api/es/lnb/forcemerge
     */
    @PutMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        log.info("[LNB] forcemerge 요청")
        val result = indexingService.forcemerge()
        return ResponseEntity.ok(result)
    }

    /**
     * 인덱스 카운트
     * GET /api/es/lnb/count
     */
    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        val total = indexingService.count()
        return ResponseEntity.ok(mapOf("total" to total))
    }

    /**
     * 인덱스 삭제
     * DELETE /api/es/lnb
     */
    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        log.info("[LNB] deleteIndex 요청")
        val result = indexingService.deleteIndex()
        return ResponseEntity.ok(result)
    }
}
