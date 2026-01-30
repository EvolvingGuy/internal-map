package com.datahub.geo_poc.controller.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LC vs LNB vs LNBT 비교 Page Controller
 */
@Controller
@RequestMapping("/page/es/compare/agg")
class CompareAggWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LC vs LNB vs LNBT 비교 - 시도")
        model.addAttribute("lcApiPath", "/api/es/lc/agg/sd")
        model.addAttribute("lnbApiPath", "/api/es/lnb/agg/sd")
        model.addAttribute("lnbtApiPath", "/api/es/lnbt/agg/sd")
        model.addAttribute("ldrcApiPath", "/api/es/ldrc/clusters?level=SD")
        model.addAttribute("defaultZoom", 7)
        return "es/compare/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LC vs LNB vs LNBT 비교 - 시군구")
        model.addAttribute("lcApiPath", "/api/es/lc/agg/sgg")
        model.addAttribute("lnbApiPath", "/api/es/lnb/agg/sgg")
        model.addAttribute("lnbtApiPath", "/api/es/lnbt/agg/sgg")
        model.addAttribute("ldrcApiPath", "/api/es/ldrc/clusters?level=SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/compare/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LC vs LNB vs LNBT 비교 - 읍면동")
        model.addAttribute("lcApiPath", "/api/es/lc/agg/emd")
        model.addAttribute("lnbApiPath", "/api/es/lnb/agg/emd")
        model.addAttribute("lnbtApiPath", "/api/es/lnbt/agg/emd")
        model.addAttribute("ldrcApiPath", "/api/es/ldrc/clusters?level=EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/compare/agg"
    }
}
