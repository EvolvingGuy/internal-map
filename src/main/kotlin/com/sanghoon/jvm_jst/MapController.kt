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
    @GetMapping("/h3/redis/emd")
    fun h3RedisEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_redis_emd"
    }

    @GetMapping("/h3/redis/region/emd")
    fun h3RedisRegionEmd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_redis_region_emd"
    }

    @GetMapping("/h3/redis/region/emd9")
    fun h3RedisRegionEmd9(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_redis_region_emd9"
    }

    @GetMapping("/h3/redis/cell/emd9")
    fun h3RedisCellEmd9(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_redis_cell_emd9"
    }

    @GetMapping("/h3/redis/grid/emd9")
    fun h3RedisGridEmd9(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_redis_grid_emd9"
    }

    @GetMapping("/h3/redis/region/sgg8")
    fun h3RedisRegionSgg8(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_redis_region_sgg8"
    }

    @GetMapping("/h3/redis/region/sd6")
    fun h3RedisRegionSd6(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        return "h3_redis_region_sd6"
    }
}
