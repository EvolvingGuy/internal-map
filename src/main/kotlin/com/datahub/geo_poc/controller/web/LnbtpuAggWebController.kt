package com.datahub.geo_poc.controller.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LNBTPU Aggregation Page Controller
 * 4개 파티션 인덱스 조회 페이지 (lnbtpu_*) - 균등분배
 */
@Controller
@RequestMapping("/page/es/lnbtpu/agg")
class LnbtpuAggWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPU Aggregation - 시도 (균등분배)")
        model.addAttribute("apiPath", "/api/es/lnbtpu/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lnbtpu/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPU Aggregation - 시군구 (균등분배)")
        model.addAttribute("apiPath", "/api/es/lnbtpu/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lnbtpu/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPU Aggregation - 읍면동 (균등분배)")
        model.addAttribute("apiPath", "/api/es/lnbtpu/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lnbtpu/agg"
    }
}
