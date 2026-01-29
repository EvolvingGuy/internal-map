package com.datahub.geo_poc.controller.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LNBTPS Aggregation Page Controller
 * 17개 시도별 인덱스 조회 페이지 (lnbtps_*)
 */
@Controller
@RequestMapping("/page/es/lnbtps/agg")
class LnbtpsAggWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPS Aggregation - 시도 (SD별 17개 인덱스)")
        model.addAttribute("apiPath", "/api/es/lnbtps/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lnbtps/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPS Aggregation - 시군구 (SD별 17개 인덱스)")
        model.addAttribute("apiPath", "/api/es/lnbtps/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lnbtps/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPS Aggregation - 읍면동 (SD별 17개 인덱스)")
        model.addAttribute("apiPath", "/api/es/lnbtps/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lnbtps/agg"
    }
}
