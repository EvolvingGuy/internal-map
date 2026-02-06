package com.datahub.geo_poc.es.l4x4nofm

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * L_4x4_NOFM 관리 컨트롤러 (단일 building/trade, forcemerge 없음)
 */
@RestController
@RequestMapping("/api/es/l-4x4-nofm")
class L4x4nofmRestController(
    private val indexingService: L4x4nofmIndexingService
) {
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.reindex())
    }

    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        return ResponseEntity.ok(indexingService.count())
    }

    @DeleteMapping
    fun delete(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.deleteIndex())
    }
}
