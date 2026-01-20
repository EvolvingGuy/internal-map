package com.sanghoon.jvm_jst.es.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/page/es/h3agg")
class H3AggPageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/sd")
    fun sd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("level", "SD")
        model.addAttribute("levelName", "시도")
        return "es/h3agg"
    }

    @GetMapping("/sgg")
    fun sgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("level", "SGG")
        model.addAttribute("levelName", "시군구")
        return "es/h3agg"
    }

    @GetMapping("/emd")
    fun emd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("level", "EMD")
        model.addAttribute("levelName", "읍면동")
        return "es/h3agg"
    }
}
