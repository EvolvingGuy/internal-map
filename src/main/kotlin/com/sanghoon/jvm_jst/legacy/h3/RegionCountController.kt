package com.sanghoon.jvm_jst.legacy.h3

import org.springframework.web.bind.annotation.*

// @RestController  // legacy - disabled
@RequestMapping("/api/region-count")
class RegionCountController(
    private val regionCountService: RegionCountService
) {

    /**
     * 읍면동별 PNU 카운트
     */
    @GetMapping("/emd")
    fun getEmdCounts(request: H3AggRequest): RegionCountResponse {
        return regionCountService.getEmdCounts(request.toBBox())
    }

    /**
     * 시군구별 PNU 카운트
     */
    @GetMapping("/sgg")
    fun getSggCounts(request: H3AggRequest): RegionCountResponse {
        return regionCountService.getSggCounts(request.toBBox())
    }

    /**
     * 시도별 PNU 카운트
     */
    @GetMapping("/sd")
    fun getSdCounts(request: H3AggRequest): RegionCountResponse {
        return regionCountService.getSdCounts(request.toBBox())
    }
}
