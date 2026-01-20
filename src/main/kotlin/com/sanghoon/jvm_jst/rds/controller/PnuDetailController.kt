package com.sanghoon.jvm_jst.rds.controller

import com.sanghoon.jvm_jst.rds.common.AggRequest
import com.sanghoon.jvm_jst.rds.service.PnuDetailResponse
import com.sanghoon.jvm_jst.rds.service.PnuDetailService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

/**
 * PNU 상세 조회 API 컨트롤러
 */
@RestController
@RequestMapping("/api/pnu")
class PnuDetailApiController(
    private val pnuDetailService: PnuDetailService
) {
    @GetMapping("/detail")
    fun getDetail(request: AggRequest): PnuDetailResponse =
        pnuDetailService.getPnus(request.toBBox())
}

/**
 * PNU 상세 조회 페이지 컨트롤러
 */
@Controller
@RequestMapping("/page/pnu")
class PnuDetailPageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/detail")
    fun detail(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "PNU Detail")
        return "rds/detail"
    }
}
