package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbtpp.LnbtppIndexingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LNBTPP (Land Nested Building Trade Point Partitioned) 인덱싱 컨트롤러
 * 4개 파티션 인덱스 (lnbtp_1 ~ lnbtp_4) 관리
 */
@RestController
@RequestMapping("/api/es/lnbtpp")
class LnbtppRestController(
    private val indexingService: LnbtppIndexingService
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
