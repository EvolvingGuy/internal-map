package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.model.LsrcQueryResponse
import com.datahub.geo_poc.es.service.lsrc.LsrcQueryService
import com.datahub.geo_poc.model.BBoxRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LSRC 고정형 행정구역 클러스터 조회 API
 */
@RestController
@RequestMapping("/api/es/lsrc/query")
class LsrcQueryRestController(
    private val queryService: LsrcQueryService
) {
    /**
     * 시도 레벨 조회
     * GET /api/es/lsrc/query/sd?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/sd")
    fun querySd(@ModelAttribute bbox: BBoxRequest): ResponseEntity<LsrcQueryResponse> {
        val result = queryService.findByBbox("SD", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat)
        return ResponseEntity.ok(result)
    }

    /**
     * 시군구 레벨 조회
     * GET /api/es/lsrc/query/sgg?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/sgg")
    fun querySgg(@ModelAttribute bbox: BBoxRequest): ResponseEntity<LsrcQueryResponse> {
        val result = queryService.findByBbox("SGG", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat)
        return ResponseEntity.ok(result)
    }

    /**
     * 읍면동 레벨 조회
     * GET /api/es/lsrc/query/emd?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/emd")
    fun queryEmd(@ModelAttribute bbox: BBoxRequest): ResponseEntity<LsrcQueryResponse> {
        val result = queryService.findByBbox("EMD", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat)
        return ResponseEntity.ok(result)
    }
}