package com.datahub.geo_poc.es.lbt17x4regionnofm

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LBT_17x4_REGION_NOFM 관리 컨트롤러 (forcemerge 없음)
 */
@RestController
@RequestMapping("/api/es/lbt-17x4-region-nofm")
class Lbt17x4regionNofmRestController(
    private val indexingService: Lbt17x4regionNofmIndexingService
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
