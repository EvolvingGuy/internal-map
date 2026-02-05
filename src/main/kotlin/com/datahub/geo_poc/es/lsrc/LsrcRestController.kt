package com.datahub.geo_poc.es.lsrc

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/es/lsrc")
class LsrcRestController(
    private val indexingService: LsrcIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[LSRC] reindex 요청")
        return ResponseEntity.ok(indexingService.reindex())
    }

    @PutMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        log.info("[LSRC] forcemerge 요청")
        return ResponseEntity.ok(indexingService.forcemerge())
    }

    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        return ResponseEntity.ok(mapOf("total" to indexingService.count()))
    }

    @DeleteMapping
    fun deleteIndex(): ResponseEntity<Map<String, Any>> {
        log.info("[LSRC] deleteIndex 요청")
        return ResponseEntity.ok(indexingService.deleteIndex())
    }
}
