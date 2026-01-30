package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbtpg.LnbtpgIndexingService
import com.datahub.geo_poc.es.service.lnbtpg.LnbtpgQueryService
import org.springframework.web.bind.annotation.*

/**
 * LNBTPG REST Controller
 * 관리 API + Marker API
 */
@RestController
@RequestMapping("/api/es/lnbtpg")
class LnbtpgRestController(
    private val indexingService: LnbtpgIndexingService,
    private val queryService: LnbtpgQueryService
) {
    // ==================== 관리 API ====================

    @PutMapping("/reindex")
    fun reindex(): Map<String, Any> = indexingService.reindex()

    @PutMapping("/forcemerge")
    fun forcemerge(): Map<String, Any> = indexingService.forcemerge()

    @GetMapping("/count")
    fun count(): Map<String, Any> = mapOf(
        "index" to LnbtpgIndexingService.INDEX_NAME,
        "count" to indexingService.count()
    )

    @DeleteMapping
    fun deleteIndex(): Map<String, Any> = indexingService.deleteIndex()

    // ==================== Marker API ====================

    @GetMapping("/markers")
    fun markers(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double,
        @RequestParam(defaultValue = "500") limit: Int
    ): LnbtpgQueryService.MarkerResponse {
        return queryService.findMarkers(swLng, swLat, neLng, neLat, limit)
    }
}
