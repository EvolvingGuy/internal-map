package com.sanghoon.jvm_jst

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MapController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String,
    @Value("\${naver.map.client-secret:}") private val naverMapClientSecret: String
) {

    @GetMapping
    fun map(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("naverMapClientSecret", naverMapClientSecret)
        return "map"
    }

    @GetMapping("/aa")
    fun aa(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("naverMapClientSecret", naverMapClientSecret)
        return "map"
    }
}
