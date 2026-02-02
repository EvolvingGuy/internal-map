package com.datahub.geo_poc.es.lbt17x4regionfm

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LBT_17x4_REGION_FM Aggregation Page Controller
 */
@Controller
@RequestMapping("/page/es/lbt-17x4-region-fm/agg")
class Lbt17x4regionFmWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_17x4_REGION_FM Aggregation - 시도 (17인덱스 4샤드 지역별, FM)")
        model.addAttribute("apiPath", "/api/es/lbt-17x4-region-fm/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lbt17x4regionfm/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_17x4_REGION_FM Aggregation - 시군구 (17인덱스 4샤드 지역별, FM)")
        model.addAttribute("apiPath", "/api/es/lbt-17x4-region-fm/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lbt17x4regionfm/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_17x4_REGION_FM Aggregation - 읍면동 (17인덱스 4샤드 지역별, FM)")
        model.addAttribute("apiPath", "/api/es/lbt-17x4-region-fm/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lbt17x4regionfm/agg"
    }
}
