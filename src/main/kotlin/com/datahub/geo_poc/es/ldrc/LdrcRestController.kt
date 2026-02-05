package com.datahub.geo_poc.es.ldrc

import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LdrcResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/es/ldrc")
class LdrcRestController(
    private val indexingService: LdrcIndexingService,
    private val queryService: LdrcQueryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex 요청")
        return ResponseEntity.ok(indexingService.reindex())
    }

    @PutMapping("/reindex/emd")
    fun reindexEmd(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex EMD 요청")
        return ResponseEntity.ok(indexingService.reindexEmd())
    }

    @PutMapping("/reindex/sgg")
    fun reindexSgg(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex SGG 요청")
        return ResponseEntity.ok(indexingService.reindexSgg())
    }

    @PutMapping("/reindex/sd")
    fun reindexSd(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] reindex SD 요청")
        return ResponseEntity.ok(indexingService.reindexSd())
    }

    @PutMapping("/forcemerge")
    fun forcemerge(): ResponseEntity<Map<String, Any>> {
        log.info("[LDRC] forcemerge 요청")
        return ResponseEntity.ok(indexingService.forcemerge())
    }

    @GetMapping("/clusters")
    fun getClusters(
        @ModelAttribute bbox: BBoxRequest,
        @RequestParam(defaultValue = "SD") level: String
    ): ResponseEntity<LdrcResponse> {
        val upperLevel = level.uppercase()
        log.info("[LDRC] 조회: level={}", upperLevel)
        val response = queryService.queryByBBox(bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat, upperLevel)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        return ResponseEntity.ok(mapOf("total" to indexingService.count()))
    }
}
