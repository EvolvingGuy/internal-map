package com.datahub.geo_poc.es.lbt1x16fm

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LBT_1x16_FM 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/es/lbt-1x16-fm")
class Lbt1x16fmRestController(
    private val indexingService: Lbt1x16fmIndexingService
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
        return ResponseEntity.ok(mapOf("count" to indexingService.count()))
    }

    @DeleteMapping
    fun delete(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.deleteIndex())
    }
}
