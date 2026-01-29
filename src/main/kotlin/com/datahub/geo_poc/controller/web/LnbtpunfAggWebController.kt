package com.datahub.geo_poc.controller.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LNBTPUNF Aggregation Page Controller
 * 4개 파티션 인덱스 조회 페이지 (lnbtpunf_*) - 균등분배, forcemerge 미적용
 */
@Controller
@RequestMapping("/page/es/lnbtpunf/agg")
class LnbtpunfAggWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPUNF Aggregation - 시도 (균등분배, No Forcemerge)")
        model.addAttribute("apiPath", "/api/es/lnbtpunf/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lnbtpunf/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPUNF Aggregation - 시군구 (균등분배, No Forcemerge)")
        model.addAttribute("apiPath", "/api/es/lnbtpunf/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lnbtpunf/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPUNF Aggregation - 읍면동 (균등분배, No Forcemerge)")
        model.addAttribute("apiPath", "/api/es/lnbtpunf/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lnbtpunf/agg"
    }
}
