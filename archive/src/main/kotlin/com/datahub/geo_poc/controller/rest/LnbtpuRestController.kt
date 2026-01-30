package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbtpu.LnbtpuIndexingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LNBTPU (Land Nested Building Trade Point Uniform) 인덱싱 컨트롤러
 * 4개 파티션 인덱스 (lnbtpu_1 ~ lnbtpu_4) 관리 - 균등분배 (PNU hash)
 */
@RestController
@RequestMapping("/api/es/lnbtpu")
class LnbtpuRestController(
    private val indexingService: LnbtpuIndexingService
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
