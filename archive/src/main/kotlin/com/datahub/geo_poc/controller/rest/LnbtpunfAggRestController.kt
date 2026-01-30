package com.datahub.geo_poc.controller.rest

import com.datahub.geo_poc.es.service.lnbtpunf.LnbtpunfAggregationService
import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LcAggFilterRequest
import com.datahub.geo_poc.model.LcAggResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LNBTPUNF (Land Nested Building Trade Point Uniform No Forcemerge) Aggregation 컨트롤러
 * 4개 파티션 인덱스 동시 조회 (lnbtpunf_*) - 균등분배
 */
@RestController
@RequestMapping("/api/es/lnbtpunf/agg")
class LnbtpunfAggRestController(
    private val aggService: LnbtpunfAggregationService
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
