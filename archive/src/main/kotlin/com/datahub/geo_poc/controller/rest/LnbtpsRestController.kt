package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbtps.LnbtpsIndexingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LNBTPS (Land Nested Building Trade Point SD) 관리 컨트롤러
 * 17개 시도별 인덱스 관리 (lnbtps_*)
 */
@RestController
@RequestMapping("/api/es/lnbtps")
class LnbtpsRestController(
    private val indexingService: LnbtpsIndexingService
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
