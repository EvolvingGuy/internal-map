package com.datahub.geo_poc.es.lbt4x4nofm

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LBT_4x4_NOFM Aggregation Page Controller
 */
@Controller
@RequestMapping("/page/es/lbt-4x4-nofm/agg")
class Lbt4x4nofmWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_4x4_NOFM Aggregation - 시도 (4인덱스 4샤드, NOFM)")
        model.addAttribute("apiPath", "/api/es/lbt-4x4-nofm/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lbt4x4nofm/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_4x4_NOFM Aggregation - 시군구 (4인덱스 4샤드, NOFM)")
        model.addAttribute("apiPath", "/api/es/lbt-4x4-nofm/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lbt4x4nofm/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_4x4_NOFM Aggregation - 읍면동 (4인덱스 4샤드, NOFM)")
        model.addAttribute("apiPath", "/api/es/lbt-4x4-nofm/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lbt4x4nofm/agg"
    }
}
