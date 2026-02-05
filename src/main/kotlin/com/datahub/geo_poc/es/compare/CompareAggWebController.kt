package com.datahub.geo_poc.es.compare

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LSRC vs LDRC vs LBT17x4 비교 Page Controller
 * 레벨별 3페이지 (SD, SGG, EMD)
 */
@Controller
@RequestMapping("/page/es/compare/agg")
class CompareAggWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun pageSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LSRC vs LDRC vs LBT17x4 비교 - 시도")
        model.addAttribute("lsrcApiPath", "/api/es/lsrc/query/sd")
        model.addAttribute("ldrcApiPath", "/api/es/ldrc/clusters?level=SD")
        model.addAttribute("lbtApiPath", "/api/es/lbt-17x4-region-nofm/agg/sd")
        model.addAttribute("defaultZoom", 7)
        return "es/compare/agg"
    }

    @GetMapping("/sgg")
    fun pageSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LSRC vs LDRC vs LBT17x4 비교 - 시군구")
        model.addAttribute("lsrcApiPath", "/api/es/lsrc/query/sgg")
        model.addAttribute("ldrcApiPath", "/api/es/ldrc/clusters?level=SGG")
        model.addAttribute("lbtApiPath", "/api/es/lbt-17x4-region-nofm/agg/sgg")
        model.addAttribute("defaultZoom", 10)
        return "es/compare/agg"
    }

    @GetMapping("/emd")
    fun pageEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "LSRC vs LDRC vs LBT17x4 비교 - 읍면동")
        model.addAttribute("lsrcApiPath", "/api/es/lsrc/query/emd")
        model.addAttribute("ldrcApiPath", "/api/es/ldrc/clusters?level=EMD")
        model.addAttribute("lbtApiPath", "/api/es/lbt-17x4-region-nofm/agg/emd")
        model.addAttribute("defaultZoom", 14)
        return "es/compare/agg"
    }
}
