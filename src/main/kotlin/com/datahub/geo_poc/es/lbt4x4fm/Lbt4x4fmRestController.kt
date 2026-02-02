package com.datahub.geo_poc.es.lbt4x4fm

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LBT_4x4_FM 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/es/lbt-4x4-fm")
class Lbt4x4fmRestController(
    private val indexingService: Lbt4x4fmIndexingService
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
