package com.datahub.geo_poc.es.lbt1x16nofm

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LBT_1x16_NOFM Aggregation Page Controller
 */
@Controller
@RequestMapping("/page/es/lbt-1x16-nofm/agg")
class Lbt1x16nofmWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_1x16_NOFM Aggregation - 시도 (단일 인덱스 16샤드, NOFM)")
        model.addAttribute("apiPath", "/api/es/lbt-1x16-nofm/agg/sd")
        model.addAttribute("level", "SD")
        model.addAttribute("defaultZoom", 7)
        return "es/lbt1x16nofm/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_1x16_NOFM Aggregation - 시군구 (단일 인덱스 16샤드, NOFM)")
        model.addAttribute("apiPath", "/api/es/lbt-1x16-nofm/agg/sgg")
        model.addAttribute("level", "SGG")
        model.addAttribute("defaultZoom", 10)
        return "es/lbt1x16nofm/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LBT_1x16_NOFM Aggregation - 읍면동 (단일 인덱스 16샤드, NOFM)")
        model.addAttribute("apiPath", "/api/es/lbt-1x16-nofm/agg/emd")
        model.addAttribute("level", "EMD")
        model.addAttribute("defaultZoom", 14)
        return "es/lbt1x16nofm/agg"
    }
}
