package com.datahub.geo_poc.es.lbt2x3nofm

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LBT_2x3_NOFM 관리 컨트롤러 (forcemerge 없음)
 */
@RestController
@RequestMapping("/api/es/lbt-2x3-nofm")
class Lbt2x3nofmRestController(
    private val indexingService: Lbt2x3nofmIndexingService
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
