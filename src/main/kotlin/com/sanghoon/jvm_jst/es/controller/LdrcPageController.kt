package com.sanghoon.jvm_jst.es.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * LDRC 가변형 행정구역 클러스터 페이지 컨트롤러
 */
@Controller
@RequestMapping("/page/es/ldrc")
class LdrcPageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun sd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "가변형 시도")
        model.addAttribute("apiPath", "/api/es/ldrc/query/sd")
        model.addAttribute("circleSize", 80)
        model.addAttribute("fontSize", 14)
        return "es/ldrc/region"
    }

    @GetMapping("/sgg")
    fun sgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "가변형 시군구")
        model.addAttribute("apiPath", "/api/es/ldrc/query/sgg")
        model.addAttribute("circleSize", 60)
        model.addAttribute("fontSize", 12)
        return "es/ldrc/region"
    }

    @GetMapping("/emd")
    fun emd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "가변형 읍면동")
        model.addAttribute("apiPath", "/api/es/ldrc/query/emd")
        model.addAttribute("circleSize", 50)
        model.addAttribute("fontSize", 11)
        return "es/ldrc/region"
    }
}
