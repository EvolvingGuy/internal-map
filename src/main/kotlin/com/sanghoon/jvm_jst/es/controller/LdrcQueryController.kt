package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.service.LdrcQueryResult
import com.sanghoon.jvm_jst.es.service.LdrcQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LDRC 가변형 행정구역 클러스터 조회 API
 */
@RestController
@RequestMapping("/api/es/ldrc/query")
class LdrcQueryController(
    private val queryService: LdrcQueryService
) {
    /**
     * 시도 레벨 조회
     * GET /api/es/ldrc/query/sd?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/sd")
    fun querySd(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double
    ): ResponseEntity<LdrcQueryResult> {
        val result = queryService.findByBbox("SD", swLng, swLat, neLng, neLat)
        return ResponseEntity.ok(result)
    }

    /**
     * 시군구 레벨 조회
     * GET /api/es/ldrc/query/sgg?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/sgg")
    fun querySgg(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double
    ): ResponseEntity<LdrcQueryResult> {
        val result = queryService.findByBbox("SGG", swLng, swLat, neLng, neLat)
        return ResponseEntity.ok(result)
    }

    /**
     * 읍면동 레벨 조회
     * GET /api/es/ldrc/query/emd?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/emd")
    fun queryEmd(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double
    ): ResponseEntity<LdrcQueryResult> {
        val result = queryService.findByBbox("EMD", swLng, swLat, neLng, neLat)
        return ResponseEntity.ok(result)
    }
}
