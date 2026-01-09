package com.sanghoon.jvm_jst.h3

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/h3")
class H3AggController(
    private val h3AggService: H3AggService
) {

    // ===================== H3 격자 단위 =====================

    @GetMapping("/cells/emd")
    fun getEmdCells(request: H3AggRequest): H3AggResponse {
        return h3AggService.getEmdCells(request.toBBox())
    }

    @GetMapping("/cells/sgg")
    fun getSggCells(request: H3AggRequest): H3AggResponse {
        return h3AggService.getSggCells(request.toBBox())
    }

    @GetMapping("/cells/sd")
    fun getSdCells(request: H3AggRequest): H3AggResponse {
        return h3AggService.getSdCells(request.toBBox())
    }

    // ===================== 행정구역 집계 =====================

    @GetMapping("/region/emd")
    fun getEmdAggregation(request: H3AggRequest): RegionAggResponse {
        return h3AggService.getEmdAggregation(request.toBBox())
    }

    @GetMapping("/region/sgg")
    fun getSggAggregation(request: H3AggRequest): RegionAggResponse {
        return h3AggService.getSggAggregation(request.toBBox())
    }

    @GetMapping("/region/sd")
    fun getSdAggregation(request: H3AggRequest): RegionAggResponse {
        return h3AggService.getSdAggregation(request.toBBox())
    }

}

data class H3AggRequest(
    val swLng: Double,
    val swLat: Double,
    val neLng: Double,
    val neLat: Double
) {
    fun toBBox() = BBox(swLng, swLat, neLng, neLat)
}
