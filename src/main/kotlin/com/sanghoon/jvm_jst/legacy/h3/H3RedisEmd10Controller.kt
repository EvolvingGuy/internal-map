package com.sanghoon.jvm_jst.legacy.h3

import org.springframework.web.bind.annotation.*

// @RestController  // legacy - disabled
@RequestMapping("/api/h3/redis-emd10")
class H3RedisEmd10Controller(
    private val h3RedisEmd10Service: H3RedisEmd10Service
) {

    @GetMapping("/region")
    fun getRegionAggregation(request: H3AggRequest): H3RegionResponse {
        return h3RedisEmd10Service.getRegionAggregation(request.toBBox())
    }
}
