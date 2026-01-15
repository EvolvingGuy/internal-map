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

    @GetMapping("/h3/emd")
    fun h3Emd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_emd"
    }

    @GetMapping("/h3/sgg")
    fun h3Sgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_sgg"
    }

    @GetMapping("/h3/sd")
    fun h3Sd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_sd"
    }

    // 행정구역 집계 지도
    @GetMapping("/h3/region/emd")
    fun h3RegionEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_region_emd"
    }

    @GetMapping("/h3/region/sgg")
    fun h3RegionSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_region_sgg"
    }

    @GetMapping("/h3/region/sd")
    fun h3RegionSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_region_sd"
    }

    // H3 Redis 캐시 지도
    @GetMapping("/h3/jvm/emd")
    fun h3RedisEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_jvm_emd"
    }

    @GetMapping("/h3/jvm/region/emd")
    fun h3RedisRegionEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_jvm_region_emd"
    }

    @GetMapping("/h3/jvm/region/emd10")
    fun h3RedisRegionEmd10(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_jvm_region_emd10"
    }

    @GetMapping("/h3/jvm/cell/emd10")
    fun h3RedisCellEmd10(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_jvm_cell_emd10"
    }

    @GetMapping("/h3/jvm/grid/emd10")
    fun h3RedisGridEmd10(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_jvm_grid_emd10"
    }

    @GetMapping("/h3/jvm/region/sgg8")
    fun h3RedisRegionSgg8(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_jvm_region_sgg8"
    }

    @GetMapping("/h3/jvm/region/sd6")
    fun h3RedisRegionSd6(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_jvm_region_sd6"
    }

    // Region Count (고정 카운트)
    @GetMapping("/region-count/emd")
    fun regionCountEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "region_count_emd"
    }

    @GetMapping("/region-count/sgg")
    fun regionCountSgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "region_count_sgg"
    }

    @GetMapping("/region-count/sd")
    fun regionCountSd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "region_count_sd"
    }

    // 고정 그리드 (300m)
    @GetMapping("/h3/fixed-grid")
    fun h3FixedGrid(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_fixed_grid"
    }

    // 뷰포트 기반 동적 그리드
    @GetMapping("/h3/viewport-grid")
    fun h3ViewportGrid(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_viewport_grid"
    }

    // Redis Protobuf - 읍면동 집계
    @GetMapping("/h3/proto/region/emd10")
    fun h3ProtoRegionEmd10(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_proto_emd10_region"
    }
}
