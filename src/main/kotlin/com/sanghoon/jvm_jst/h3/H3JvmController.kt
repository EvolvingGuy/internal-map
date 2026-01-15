package com.sanghoon.jvm_jst.h3

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/h3/jvm")
class H3JvmController(
    private val h3JvmService: H3JvmService
) {

    @GetMapping("/emd")
    fun getEmdAggregation(request: H3AggRequest): H3JvmResponse {
        return h3JvmService.getEmdAggregation(request.toBBox())
    }

    @GetMapping("/sgg")
    fun getSggAggregation(request: H3AggRequest): H3JvmResponse {
        return h3JvmService.getSggAggregation(request.toBBox())
    }

    @GetMapping("/sd")
    fun getSdAggregation(request: H3AggRequest): H3JvmResponse {
        return h3JvmService.getSdAggregation(request.toBBox())
    }

    @GetMapping("/region/emd")
    fun getEmdRegionAggregation(request: H3AggRequest): H3RegionResponse {
        return h3JvmService.getEmdRegionAggregation(request.toBBox())
    }

    @GetMapping("/region/emd10")
    fun getEmd10RegionAggregation(request: H3AggRequest): H3RegionResponse {
        return h3JvmService.getEmd10RegionAggregation(request.toBBox())
    }

    @GetMapping("/cell/emd10")
    fun getEmd10CellAggregation(request: H3AggRequest): H3JvmResponse {
        return h3JvmService.getEmd10CellAggregation(request.toBBox())
    }

    @GetMapping("/grid/emd10")
    fun getEmd10GridAggregation(request: H3AggRequest): H3JvmResponse {
        return h3JvmService.getEmd10GridAggregation(request.toBBox())
    }

    @GetMapping("/region/sgg8")
    fun getSgg8RegionAggregation(request: H3AggRequest): H3RegionResponse {
        return h3JvmService.getSgg8RegionAggregation(request.toBBox())
    }

    @GetMapping("/region/sd6")
    fun getSd6RegionAggregation(request: H3AggRequest): H3RegionResponse {
        return h3JvmService.getSd6RegionAggregation(request.toBBox())
    }

    @GetMapping("/fixed-grid")
    fun getFixedGridAggregation(
        request: H3AggRequest,
        @RequestParam(defaultValue = "300") gridSize: Double
    ): H3FixedGridResponse {
        return h3JvmService.getFixedGridAggregation(request.toBBox(), gridSize)
    }

    @GetMapping("/viewport-grid")
    fun getViewportGridAggregation(
        request: H3AggRequest,
        @RequestParam viewportWidth: Int,
        @RequestParam viewportHeight: Int,
        @RequestParam(defaultValue = "450") targetCellSize: Int
    ): H3ViewportGridResponse {
        return h3JvmService.getViewportGridAggregation(
            request.toBBox(),
            viewportWidth,
            viewportHeight,
            targetCellSize
        )
    }
}
