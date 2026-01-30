package com.datahub.geo_poc.controller.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LNBTP16 Aggregation Page Controller
 * 단일 인덱스 16샤드 조회 페이지
 */
@Controller
@RequestMapping("/page/es/lnbtp16/agg")
class Lnbtp16AggWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTP16 Aggregation - 시도 (단일 인덱스 16샤드)")
        model.addAttribute("apiPath", "/api/es/lnbtp16/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lnbtp16/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTP16 Aggregation - 시군구 (단일 인덱스 16샤드)")
        model.addAttribute("apiPath", "/api/es/lnbtp16/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lnbtp16/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTP16 Aggregation - 읍면동 (단일 인덱스 16샤드)")
        model.addAttribute("apiPath", "/api/es/lnbtp16/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lnbtp16/agg"
    }
}
