package com.datahub.geo_poc.controller.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LSRC 고정형 행정구역 클러스터 페이지 컨트롤러
 */
@Controller
@RequestMapping("/page/es/lsrc")
class LsrcWebController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun sd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "고정형 시도")
        model.addAttribute("apiPath", "/api/es/lsrc/query/sd")
        model.addAttribute("circleSize", 80)
        model.addAttribute("fontSize", 14)
        return "es/lsrc/region"
    }

    @GetMapping("/sgg")
    fun sgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "고정형 시군구")
        model.addAttribute("apiPath", "/api/es/lsrc/query/sgg")
        model.addAttribute("circleSize", 60)
        model.addAttribute("fontSize", 12)
        return "es/lsrc/region"
    }

    @GetMapping("/emd")
    fun emd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "고정형 읍면동")
        model.addAttribute("apiPath", "/api/es/lsrc/query/emd")
        model.addAttribute("circleSize", 50)
        model.addAttribute("fontSize", 11)
        return "es/lsrc/region"
    }
}
