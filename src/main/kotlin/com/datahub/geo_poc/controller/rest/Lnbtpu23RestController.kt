package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbtpu23.Lnbtpu23IndexingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LNBTPU23 (Land Nested Building Trade Point Uniform 2-Index 3-Shard) 인덱싱 컨트롤러
 * 2개 파티션 인덱스 (lnbtpu23_1 ~ lnbtpu23_2) 관리 - 균등분배 (PNU hash)
 */
@RestController
@RequestMapping("/api/es/lnbtpu23")
class Lnbtpu23RestController(
    private val indexingService: Lnbtpu23IndexingService
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
        return ResponseEntity.ok(mapOf("counts" to indexingService.count()))
    }

    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.deleteIndex())
    }
}
