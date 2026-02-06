package com.datahub.geo_poc.es.lb4x4nofm

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LB_4x4_NOFM 관리 컨트롤러
 * - buildings=nested, trade=flat(top1)
 */
@RestController
@RequestMapping("/api/es/lb-4x4-nofm")
class Lb4x4nofmRestController(
    private val indexingService: Lb4x4nofmIndexingService
) {
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.reindex())
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
