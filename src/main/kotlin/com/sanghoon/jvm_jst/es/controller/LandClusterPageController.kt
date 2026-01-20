package com.sanghoon.jvm_jst.es.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/page/es")
class LandClusterPageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/cluster")
    fun cluster(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "es/cluster"
    }

    @GetMapping("/h3-visual")
    fun h3Visual(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "es/h3-visual"
    }
}
