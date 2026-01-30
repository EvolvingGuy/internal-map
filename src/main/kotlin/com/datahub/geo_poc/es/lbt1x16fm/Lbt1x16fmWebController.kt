package com.datahub.geo_poc.es.lbt1x16fm

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LBT_1x16_FM Aggregation Page Controller
 */
@Controller
@RequestMapping("/page/es/lbt-1x16-fm/agg")
class Lbt1x16fmWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_1x16_FM Aggregation - 시도 (단일 인덱스 16샤드, FM)")
        model.addAttribute("apiPath", "/api/es/lbt-1x16-fm/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lbt1x16fm/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_1x16_FM Aggregation - 시군구 (단일 인덱스 16샤드, FM)")
        model.addAttribute("apiPath", "/api/es/lbt-1x16-fm/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lbt1x16fm/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_1x16_FM Aggregation - 읍면동 (단일 인덱스 16샤드, FM)")
        model.addAttribute("apiPath", "/api/es/lbt-1x16-fm/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lbt1x16fm/agg"
    }
}
