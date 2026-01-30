package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbtpunf.LnbtpunfIndexingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LNBTPUNF (Land Nested Building Trade Point Uniform No Forcemerge) 인덱싱 컨트롤러
 * 4개 파티션 인덱스 (lnbtpunf_1 ~ lnbtpunf_4) 관리 - 균등분배 (PNU hash), forcemerge 미적용
 */
@RestController
@RequestMapping("/api/es/lnbtpunf")
class LnbtpunfRestController(
    private val indexingService: LnbtpunfIndexingService
) {
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.reindex())
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
