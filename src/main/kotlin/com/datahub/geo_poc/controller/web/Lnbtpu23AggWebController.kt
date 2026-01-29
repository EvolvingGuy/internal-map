package com.datahub.geo_poc.controller.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LNBTPU23 Aggregation Page Controller
 * 2개 파티션 인덱스 조회 페이지 (lnbtpu23_*) - 균등분배
 */
@Controller
@RequestMapping("/page/es/lnbtpu23/agg")
class Lnbtpu23AggWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPU23 Aggregation - 시도 (균등분배)")
        model.addAttribute("apiPath", "/api/es/lnbtpu23/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lnbtpu23/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPU23 Aggregation - 시군구 (균등분배)")
        model.addAttribute("apiPath", "/api/es/lnbtpu23/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lnbtpu23/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPU23 Aggregation - 읍면동 (균등분배)")
        model.addAttribute("apiPath", "/api/es/lnbtpu23/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lnbtpu23/agg"
    }
}
