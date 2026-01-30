package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbtp16.Lnbtp16IndexingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LNBTP16 (Land Nested Building Trade Point 16-Shard) 관리 컨트롤러
 * 단일 인덱스 16샤드 관리
 */
@RestController
@RequestMapping("/api/es/lnbtp16")
class Lnbtp16RestController(
    private val indexingService: Lnbtp16IndexingService
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
