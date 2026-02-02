package com.datahub.geo_poc.es.lbt4x4nofm

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LBT_4x4_NOFM 관리 컨트롤러 (forcemerge 없음)
 */
@RestController
@RequestMapping("/api/es/lbt-4x4-nofm")
class Lbt4x4nofmRestController(
    private val indexingService: Lbt4x4nofmIndexingService
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
