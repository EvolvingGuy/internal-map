package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.model.*
import com.datahub.geo_poc.es.service.lnb.LnbAggregationService
import com.datahub.geo_poc.es.service.lnb.LnbGridAggregationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LNB Aggregation API Controller
 * nested 타입 buildings 필터링 지원
 */
@RestController
@RequestMapping("/api/es/lnb/agg")
class LnbAggRestController(
    private val aggService: LnbAggregationService,
    private val gridAggService: LnbGridAggregationService
) {
    @GetMapping("/sd")
    fun aggregateSd(
        @ModelAttribute bbox: BBoxRequest,
        @ModelAttribute filterRequest: LcAggFilterRequest
    ): ResponseEntity<LcAggResponse> {
        return ResponseEntity.ok(aggService.aggregateBySd(bbox, filterRequest.toFilter()))
    }

    @GetMapping("/sgg")
    fun aggregateSgg(
        @ModelAttribute bbox: BBoxRequest,
        @ModelAttribute filterRequest: LcAggFilterRequest
    ): ResponseEntity<LcAggResponse> {
        return ResponseEntity.ok(aggService.aggregateBySgg(bbox, filterRequest.toFilter()))
    }

    @GetMapping("/emd")
    fun aggregateEmd(
        @ModelAttribute bbox: BBoxRequest,
        @ModelAttribute filterRequest: LcAggFilterRequest
    ): ResponseEntity<LcAggResponse> {
        return ResponseEntity.ok(aggService.aggregateByEmd(bbox, filterRequest.toFilter()))
    }

    @GetMapping("/grid")
    fun aggregateGrid(
        @ModelAttribute bbox: BBoxRequest,
        @ModelAttribute gridParams: GridParamsRequest,
        @ModelAttribute filterRequest: LcAggFilterRequest
    ): ResponseEntity<LcGridAggResponse> {
        return ResponseEntity.ok(gridAggService.aggregate(bbox, gridParams, filterRequest.toFilter()))
    }
}
