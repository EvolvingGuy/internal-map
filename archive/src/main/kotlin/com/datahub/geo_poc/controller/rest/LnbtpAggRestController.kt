package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbtp.LnbtpAggregationService
import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LcAggFilterRequest
import com.datahub.geo_poc.model.LcAggResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LNBTP (Land Nested Building Trade Point) Aggregation 컨트롤러
 */
@RestController
@RequestMapping("/api/es/lnbtp/agg")
class LnbtpAggRestController(
    private val aggService: LnbtpAggregationService
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
}
