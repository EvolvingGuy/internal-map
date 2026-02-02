package com.datahub.geo_poc.es.lbt2x3fm

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LBT_2x3_FM 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/es/lbt-2x3-fm")
class Lbt2x3fmRestController(
    private val indexingService: Lbt2x3fmIndexingService
) {
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.reindex())
    }

    @PutMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.forcemerge())
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
