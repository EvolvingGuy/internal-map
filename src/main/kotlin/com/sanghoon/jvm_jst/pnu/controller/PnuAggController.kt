package com.sanghoon.jvm_jst.pnu.controller

import com.sanghoon.jvm_jst.pnu.common.AggRequest
import com.sanghoon.jvm_jst.pnu.common.RegionLevel
import com.sanghoon.jvm_jst.pnu.service.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

/**
 * PNU Agg 컨트롤러 - 테이블 그대로 조회
 */
@RestController
@RequestMapping("/api/pnu/agg")
class PnuAggApiController(
    private val aggService: PnuAggService
) {
    @GetMapping("/emd_11")
    fun getEmd11(request: AggRequest): AggResponse = aggService.getEmd11(request.toBBox())

    @GetMapping("/emd_10")
    fun getEmd10(request: AggRequest): AggResponse = aggService.getEmd10(request.toBBox())

    @GetMapping("/emd_09")
    fun getEmd09(request: AggRequest): AggResponse = aggService.getEmd09(request.toBBox())

    @GetMapping("/sgg_08")
    fun getSgg08(request: AggRequest): AggResponse = aggService.getSgg08(request.toBBox())

    @GetMapping("/sgg_07")
    fun getSgg07(request: AggRequest): AggResponse = aggService.getSgg07(request.toBBox())

    @GetMapping("/sd_06")
    fun getSd06(request: AggRequest): AggResponse = aggService.getSd06(request.toBBox())

    @GetMapping("/sd_05")
    fun getSd05(request: AggRequest): AggResponse = aggService.getSd05(request.toBBox())
}

/**
 * PNU Agg 가변형 컨트롤러
 */
@RestController
@RequestMapping("/api/pnu/agg/dynamic")
class PnuAggDynamicApiController(
    private val dynamicService: PnuAggDynamicService
) {
    @GetMapping("/emd_11")
    fun getEmd11(request: AggRequest): DynamicResponse = dynamicService.getEmd11(request.toBBox())

    @GetMapping("/emd_10")
    fun getEmd10(request: AggRequest): DynamicResponse = dynamicService.getEmd10(request.toBBox())

    @GetMapping("/emd_09")
    fun getEmd09(request: AggRequest): DynamicResponse = dynamicService.getEmd09(request.toBBox())

    @GetMapping("/sgg_08")
    fun getSgg08(request: AggRequest): DynamicResponse = dynamicService.getSgg08(request.toBBox())

    @GetMapping("/sgg_07")
    fun getSgg07(request: AggRequest): DynamicResponse = dynamicService.getSgg07(request.toBBox())

    @GetMapping("/sd_06")
    fun getSd06(request: AggRequest): DynamicResponse = dynamicService.getSd06(request.toBBox())

    @GetMapping("/sd_05")
    fun getSd05(request: AggRequest): DynamicResponse = dynamicService.getSd05(request.toBBox())
}

/**
 * PNU Agg 고정형 컨트롤러
 */
@RestController
@RequestMapping("/api/pnu/agg/static")
class PnuAggStaticApiController(
    private val staticService: PnuAggStaticService
) {
    @GetMapping("/emd")
    fun getEmd(request: AggRequest): StaticResponse = staticService.getEmd(request.toBBox())

    @GetMapping("/sgg")
    fun getSgg(request: AggRequest): StaticResponse = staticService.getSgg(request.toBBox())

    @GetMapping("/sd")
    fun getSd(request: AggRequest): StaticResponse = staticService.getSd(request.toBBox())
}

/**
 * 페이지 컨트롤러 - 테이블 그대로 조회
 */
@Controller
@RequestMapping("/page/pnu/agg")
class PnuAggPageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/emd_11")
    fun emd11(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/emd_11")
        model.addAttribute("title", "PNU Agg - EMD 11")
        return "pnu/agg"
    }

    @GetMapping("/emd_10")
    fun emd10(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/emd_10")
        model.addAttribute("title", "PNU Agg - EMD 10")
        return "pnu/agg"
    }

    @GetMapping("/emd_09")
    fun emd09(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/emd_09")
        model.addAttribute("title", "PNU Agg - EMD 09")
        return "pnu/agg"
    }

    @GetMapping("/sgg_08")
    fun sgg08(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/sgg_08")
        model.addAttribute("title", "PNU Agg - SGG 08")
        return "pnu/agg"
    }

    @GetMapping("/sgg_07")
    fun sgg07(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/sgg_07")
        model.addAttribute("title", "PNU Agg - SGG 07")
        return "pnu/agg"
    }

    @GetMapping("/sd_06")
    fun sd06(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/sd_06")
        model.addAttribute("title", "PNU Agg - SD 06")
        return "pnu/agg"
    }

    @GetMapping("/sd_05")
    fun sd05(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/sd_05")
        model.addAttribute("title", "PNU Agg - SD 05")
        return "pnu/agg"
    }
}

/**
 * 페이지 컨트롤러 - 가변형
 */
@Controller
@RequestMapping("/page/pnu/agg/dynamic")
class PnuAggDynamicPageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/emd_11")
    fun emd11(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/dynamic/emd_11")
        model.addAttribute("title", "PNU Dynamic - EMD 11")
        model.addAttribute("circleSize", 60)
        model.addAttribute("fontSize", 10)
        return "pnu/dynamic"
    }

    @GetMapping("/emd_10")
    fun emd10(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/dynamic/emd_10")
        model.addAttribute("title", "PNU Dynamic - EMD 10")
        model.addAttribute("circleSize", 60)
        model.addAttribute("fontSize", 10)
        return "pnu/dynamic"
    }

    @GetMapping("/emd_09")
    fun emd09(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/dynamic/emd_09")
        model.addAttribute("title", "PNU Dynamic - EMD 09")
        model.addAttribute("circleSize", 60)
        model.addAttribute("fontSize", 10)
        return "pnu/dynamic"
    }

    @GetMapping("/sgg_08")
    fun sgg08(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/dynamic/sgg_08")
        model.addAttribute("title", "PNU Dynamic - SGG 08")
        model.addAttribute("circleSize", 80)
        model.addAttribute("fontSize", 13)
        return "pnu/dynamic"
    }

    @GetMapping("/sgg_07")
    fun sgg07(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/dynamic/sgg_07")
        model.addAttribute("title", "PNU Dynamic - SGG 07")
        model.addAttribute("circleSize", 80)
        model.addAttribute("fontSize", 13)
        return "pnu/dynamic"
    }

    @GetMapping("/sd_06")
    fun sd06(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/dynamic/sd_06")
        model.addAttribute("title", "PNU Dynamic - SD 06")
        model.addAttribute("circleSize", 100)
        model.addAttribute("fontSize", 16)
        return "pnu/dynamic"
    }

    @GetMapping("/sd_05")
    fun sd05(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/dynamic/sd_05")
        model.addAttribute("title", "PNU Dynamic - SD 05")
        model.addAttribute("circleSize", 100)
        model.addAttribute("fontSize", 16)
        return "pnu/dynamic"
    }
}

/**
 * 페이지 컨트롤러 - 고정형
 */
@Controller
@RequestMapping("/page/pnu/agg/static")
class PnuAggStaticPageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/emd")
    fun emd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/static/emd")
        model.addAttribute("title", "PNU Static - EMD")
        model.addAttribute("circleSize", 60)
        model.addAttribute("fontSize", 10)
        return "pnu/static"
    }

    @GetMapping("/sgg")
    fun sgg(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/static/sgg")
        model.addAttribute("title", "PNU Static - SGG")
        model.addAttribute("circleSize", 80)
        model.addAttribute("fontSize", 13)
        return "pnu/static"
    }

    @GetMapping("/sd")
    fun sd(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("apiPath", "/api/pnu/agg/static/sd")
        model.addAttribute("title", "PNU Static - SD")
        model.addAttribute("circleSize", 100)
        model.addAttribute("fontSize", 16)
        return "pnu/static"
    }
}

/**
 * PNU Agg 그리드 API 컨트롤러
 */
@RestController
@RequestMapping("/api/pnu/agg")
class PnuAggGridApiController(
    private val gridService: PnuAggGridService
) {
    @GetMapping("/grid")
    fun getGrid(
        request: AggRequest,
        @RequestParam zoomLevel: Int,
        @RequestParam viewportWidth: Int,
        @RequestParam viewportHeight: Int
    ): GridResponse = gridService.getGrid(request.toBBox(), zoomLevel, viewportWidth, viewportHeight)
}

/**
 * 페이지 컨트롤러 - 그리드
 */
@Controller
@RequestMapping("/page/pnu/agg")
class PnuAggGridPageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/grid")
    fun grid(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "PNU Grid")
        return "pnu/grid"
    }
}

/**
 * PNU Agg 카운트 API 컨트롤러
 */
@RestController
@RequestMapping("/api/pnu/agg")
class PnuAggCountApiController(
    private val countService: PnuAggCountService
) {
    @GetMapping("/count")
    fun getCount(
        request: AggRequest,
        @RequestParam zoomLevel: Int
    ): CountResponse = countService.getCount(request.toBBox(), zoomLevel)
}

/**
 * 페이지 컨트롤러 - 카운트
 */
@Controller
@RequestMapping("/page/pnu/agg")
class PnuAggCountPageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping("/count")
    fun count(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "PNU Count")
        return "pnu/count"
    }
}

/**
 * 중심점 행정구역 인디케이터 API 컨트롤러
 */
@RestController
@RequestMapping("/api/pnu/agg")
class CenterIndicatorApiController(
    private val centerIndicatorService: CenterIndicatorService
) {
    @GetMapping("/indicator")
    fun getIndicator(request: AggRequest): CenterIndicatorResponse =
        centerIndicatorService.getIndicator(request.toBBox())
}
