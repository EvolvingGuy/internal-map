package com.datahub.geo_poc.es.lbt1x16nofm

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LBT_1x16_NOFM 관리 컨트롤러 (forcemerge 없음)
 */
@RestController
@RequestMapping("/api/es/lbt-1x16-nofm")
class Lbt1x16nofmRestController(
    private val indexingService: Lbt1x16nofmIndexingService
) {
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.reindex())
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
