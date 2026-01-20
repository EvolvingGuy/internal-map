package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.service.LsrcQueryResult
import com.sanghoon.jvm_jst.es.service.LsrcQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LSRC 고정형 행정구역 클러스터 조회 API
 */
@RestController
@RequestMapping("/api/es/lsrc/query")
class LsrcQueryController(
    private val queryService: LsrcQueryService
) {
    /**
     * 시도 레벨 조회
     * GET /api/es/lsrc/query/sd?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/sd")
    fun querySd(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double
    ): ResponseEntity<LsrcQueryResult> {
        val result = queryService.findByBbox("SD", swLng, swLat, neLng, neLat)
        return ResponseEntity.ok(result)
    }

    /**
     * 시군구 레벨 조회
     * GET /api/es/lsrc/query/sgg?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/sgg")
    fun querySgg(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double
    ): ResponseEntity<LsrcQueryResult> {
        val result = queryService.findByBbox("SGG", swLng, swLat, neLng, neLat)
        return ResponseEntity.ok(result)
    }

    /**
     * 읍면동 레벨 조회
     * GET /api/es/lsrc/query/emd?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/emd")
    fun queryEmd(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double
    ): ResponseEntity<LsrcQueryResult> {
        val result = queryService.findByBbox("EMD", swLng, swLat, neLng, neLat)
        return ResponseEntity.ok(result)
    }
}
