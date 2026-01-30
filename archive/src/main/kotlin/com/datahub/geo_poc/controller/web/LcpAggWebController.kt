package com.datahub.geo_poc.controller.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LCP Aggregation Page Controller
 */
@Controller
@RequestMapping("/page/es/lcp/agg")
class LcpAggWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LCP Aggregation - 시도")
        model.addAttribute("apiPath", "/api/es/lcp/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lcp/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LCP Aggregation - 시군구")
        model.addAttribute("apiPath", "/api/es/lcp/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lcp/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LCP Aggregation - 읍면동")
        model.addAttribute("apiPath", "/api/es/lcp/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lcp/agg"
    }
}
