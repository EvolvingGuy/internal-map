package com.datahub.geo_poc.es.lsrc

import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LsrcQueryResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/es/lsrc/query")
class LsrcQueryRestController(
    private val queryService: LsrcQueryService
) {
    @GetMapping("/sd")
    fun querySd(@ModelAttribute bbox: BBoxRequest): ResponseEntity<LsrcQueryResponse> {
        return ResponseEntity.ok(queryService.findByBbox("SD", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat))
    }

    @GetMapping("/sgg")
    fun querySgg(@ModelAttribute bbox: BBoxRequest): ResponseEntity<LsrcQueryResponse> {
        return ResponseEntity.ok(queryService.findByBbox("SGG", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat))
    }

    @GetMapping("/emd")
    fun queryEmd(@ModelAttribute bbox: BBoxRequest): ResponseEntity<LsrcQueryResponse> {
        return ResponseEntity.ok(queryService.findByBbox("EMD", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat))
    }
}
