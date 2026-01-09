package com.sanghoon.jvm_jst.h3

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/h3/redis")
class H3RedisController(
    private val h3RedisService: H3RedisService
) {

    @GetMapping("/emd")
    fun getEmdAggregation(request: H3AggRequest): H3RedisResponse {
        return h3RedisService.getEmdAggregation(request.toBBox())
    }

    @GetMapping("/sgg")
    fun getSggAggregation(request: H3AggRequest): H3RedisResponse {
        return h3RedisService.getSggAggregation(request.toBBox())
    }

    @GetMapping("/sd")
    fun getSdAggregation(request: H3AggRequest): H3RedisResponse {
        return h3RedisService.getSdAggregation(request.toBBox())
    }

    @GetMapping("/region/emd")
    fun getEmdRegionAggregation(request: H3AggRequest): H3RegionResponse {
        return h3RedisService.getEmdRegionAggregation(request.toBBox())
    }

    @GetMapping("/region/emd9")
    fun getEmd9RegionAggregation(request: H3AggRequest): H3RegionResponse {
        return h3RedisService.getEmd9RegionAggregation(request.toBBox())
    }

    @GetMapping("/cell/emd9")
    fun getEmd9CellAggregation(request: H3AggRequest): H3RedisResponse {
        return h3RedisService.getEmd9CellAggregation(request.toBBox())
    }

    @GetMapping("/grid/emd9")
    fun getEmd9GridAggregation(request: H3AggRequest): H3RedisResponse {
        return h3RedisService.getEmd9GridAggregation(request.toBBox())
    }

    @GetMapping("/region/sgg8")
    fun getSgg8RegionAggregation(request: H3AggRequest): H3RegionResponse {
        return h3RedisService.getSgg8RegionAggregation(request.toBBox())
    }

    @GetMapping("/region/sd6")
    fun getSd6RegionAggregation(request: H3AggRequest): H3RegionResponse {
        return h3RedisService.getSd6RegionAggregation(request.toBBox())
    }

    @GetMapping("/fixed-grid")
    fun getFixedGridAggregation(
        request: H3AggRequest,
        @RequestParam(defaultValue = "300") gridSize: Double
    ): H3FixedGridResponse {
        return h3RedisService.getFixedGridAggregation(request.toBBox(), gridSize)
    }
}
