package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lc.LcIndexingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

/**
 * LC (Land Compact) 인덱싱 컨트롤러
 * 필지 단위 인덱스 (비즈니스 필터용)
 */
@RestController
@RequestMapping("/api/es/lc")
class LcRestController(
    private val indexingService: LcIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 전체 재인덱싱 (백그라운드 실행)
     * PUT /api/es/lc/reindex
     */
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[LC] reindex 요청 - 백그라운드 실행 시작")
        CompletableFuture.runAsync {
            try {
                indexingService.reindex()
            } catch (e: Exception) {
                log.error("[LC] reindex 실패", e)
            }
        }
        return ResponseEntity.accepted().body(mapOf(
            "status" to "started",
            "message" to "Reindex started in background. Check /api/es/lc/count for progress."
        ))
    }

    /**
     * 특정 EMD 코드만 재인덱싱 (백그라운드 실행)
     * PUT /api/es/lc/reindex/emd?codes=47940101,47940102,48121101
     */
    @PutMapping("/reindex/emd")
    fun reindexByEmd(@RequestParam codes: String): ResponseEntity<Map<String, Any>> {
        val emdCodes = codes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (emdCodes.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to "codes parameter is required (comma-separated EMD codes)"
            ))
        }

        log.info("[LC] reindex EMD 요청 - 백그라운드 실행 시작: {}", emdCodes)
        CompletableFuture.runAsync {
            try {
                indexingService.reindexByEmdCodes(emdCodes)
            } catch (e: Exception) {
                log.error("[LC] reindex EMD 실패", e)
            }
        }
        return ResponseEntity.accepted().body(mapOf(
            "status" to "started",
            "emdCodes" to emdCodes,
            "message" to "Reindex for ${emdCodes.size} EMD codes started in background."
        ))
    }

    /**
     * forcemerge 실행
     * PUT /api/es/lc/forcemerge
     */
    @PutMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        log.info("[LC] forcemerge 요청")
        val result = indexingService.forcemerge()
        return ResponseEntity.ok(result)
    }

    /**
     * 인덱스 카운트
     * GET /api/es/lc/count
     */
    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        val total = indexingService.count()
        return ResponseEntity.ok(mapOf("total" to total))
    }

    /**
     * 인덱스 삭제
     * DELETE /api/es/lc
     */
    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        log.info("[LC] deleteIndex 요청")
        val result = indexingService.deleteIndex()
        return ResponseEntity.ok(result)
    }
}
