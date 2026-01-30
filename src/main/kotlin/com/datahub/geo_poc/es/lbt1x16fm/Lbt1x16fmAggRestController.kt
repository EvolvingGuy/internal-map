package com.datahub.geo_poc.es.lbt1x16fm

import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LcAggFilterRequest
import com.datahub.geo_poc.model.LcAggResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * LBT_1x16_FM Aggregation 컨트롤러
 */
@RestController
@RequestMapping("/api/es/lbt-1x16-fm/agg")
class Lbt1x16fmAggRestController(
    private val aggService: Lbt1x16fmAggregationService
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
