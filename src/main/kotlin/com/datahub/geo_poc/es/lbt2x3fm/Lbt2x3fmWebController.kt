package com.datahub.geo_poc.es.lbt2x3fm

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LBT_2x3_FM Aggregation Page Controller
 */
@Controller
@RequestMapping("/page/es/lbt-2x3-fm/agg")
class Lbt2x3fmWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_2x3_FM Aggregation - 시도 (2인덱스 3샤드, FM)")
        model.addAttribute("apiPath", "/api/es/lbt-2x3-fm/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lbt2x3fm/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_2x3_FM Aggregation - 시군구 (2인덱스 3샤드, FM)")
        model.addAttribute("apiPath", "/api/es/lbt-2x3-fm/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lbt2x3fm/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_2x3_FM Aggregation - 읍면동 (2인덱스 3샤드, FM)")
        model.addAttribute("apiPath", "/api/es/lbt-2x3-fm/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lbt2x3fm/agg"
    }
}
