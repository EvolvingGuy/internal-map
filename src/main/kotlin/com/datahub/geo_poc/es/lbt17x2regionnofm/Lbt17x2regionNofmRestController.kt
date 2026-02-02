package com.datahub.geo_poc.es.lbt17x2regionnofm

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LBT_17x2_REGION_NOFM 관리 컨트롤러 (forcemerge 없음)
 */
@RestController
@RequestMapping("/api/es/lbt-17x2-region-nofm")
class Lbt17x2regionNofmRestController(
    private val indexingService: Lbt17x2regionNofmIndexingService
) {
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.reindex())
    }

    @PutMapping("/reindex-missing")
    fun reindexMissing(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.reindexMissing())
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
