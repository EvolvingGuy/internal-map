package com.datahub.geo_poc.controller.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LNBTPP Aggregation Page Controller
 * 4개 파티션 인덱스 조회 페이지 (lnbtpp_*)
 */
@Controller
@RequestMapping("/page/es/lnbtpp/agg")
class LnbtppAggWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPP Aggregation - 시도")
        model.addAttribute("apiPath", "/api/es/lnbtpp/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lnbtpp/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPP Aggregation - 시군구")
        model.addAttribute("apiPath", "/api/es/lnbtpp/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lnbtpp/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LNBTPP Aggregation - 읍면동")
        model.addAttribute("apiPath", "/api/es/lnbtpp/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lnbtpp/agg"
    }
}
