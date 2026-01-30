package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbp.LnbpIndexingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LNBP (Land Nested Building Point) 인덱싱 컨트롤러
 */
@RestController
@RequestMapping("/api/es/lnbp")
class LnbpRestController(
    private val indexingService: LnbpIndexingService
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
    fun count(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf("count" to indexingService.count()))
    }

    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.deleteIndex())
    }
}
