package com.datahub.geo_poc.es.lbt4x4nofmoffri

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LBT_4x4_NOFM_OFFRI 관리 컨트롤러
 * - refresh_interval=-1로 인덱싱하여 세그먼트 비교 테스트용
 */
@RestController
@RequestMapping("/api/es/lbt-4x4-nofm-offri")
class Lbt4x4nofmoffriRestController(
    private val indexingService: Lbt4x4nofmoffriIndexingService
) {
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.reindex())
    }

    @PutMapping("/restore-refresh-interval")
    fun restoreRefreshInterval(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.restoreRefreshInterval())
    }

    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        return ResponseEntity.ok(indexingService.count())
    }

    @GetMapping("/segments")
    fun segments(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.segments())
    }

    @DeleteMapping
    fun delete(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.deleteIndex())
    }
}
