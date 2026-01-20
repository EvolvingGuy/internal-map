package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.service.IndexingStatus
import com.sanghoon.jvm_jst.es.service.LandClusterIndexingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/es/land-cluster")
class LandClusterIndexingController(
    private val indexingService: LandClusterIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 인덱싱 시작
     * PUT /api/es/land-cluster/reindex
     * PUT /api/es/land-cluster/reindex?sido=11,26,41
     */
    @PutMapping("/reindex")
    fun reindex(@RequestParam(required = false) sido: String?): ResponseEntity<Map<String, Any>> {
        val sidoCodes = sido?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        log.info("[reindex] 요청 - sidoCodes={}", sidoCodes ?: "전체")
        indexingService.reindex(sidoCodes)
        return ResponseEntity.ok(mapOf(
            "message" to "인덱싱 시작",
            "status" to "STARTED",
            "target" to (sidoCodes ?: "ALL")
        ))
    }

    /**
     * 진행 상태 조회
     * GET /api/es/land-cluster/status
     */
    @GetMapping("/status")
    fun status(): ResponseEntity<IndexingStatus> {
        return ResponseEntity.ok(indexingService.getStatus())
    }

    /**
     * 인덱스 전체 삭제
     * DELETE /api/es/land-cluster/all
     */
    @DeleteMapping("/all")
    fun deleteAll(): ResponseEntity<Map<String, Any>> {
        log.info("[deleteAll] 전체 삭제 요청")
        val success = indexingService.deleteAll()
        return ResponseEntity.ok(mapOf(
            "message" to if (success) "삭제 완료" else "삭제 실패",
            "success" to success
        ))
    }
}
