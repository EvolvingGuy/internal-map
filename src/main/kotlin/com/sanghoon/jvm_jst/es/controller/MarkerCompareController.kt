package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.dto.LcAggFilter
import com.sanghoon.jvm_jst.es.service.LandCompactGeoData
import com.sanghoon.jvm_jst.es.service.LandCompactGeoQueryService
import com.sanghoon.jvm_jst.es.service.LandCompactIntersectQueryService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

/**
 * Center Contains vs Intersects 비교 API
 */
@RestController
@RequestMapping("/api/es/markers-compare")
class MarkerCompareApiController(
    private val geoQueryService: LandCompactGeoQueryService,
    private val intersectQueryService: LandCompactIntersectQueryService
) {
    /**
     * 두 방식 비교 조회
     * - center: geo_bounding_box (center point가 bbox 내에 있는 필지)
     * - intersect: geo_shape intersects (geometry가 bbox와 교차하는 필지)
     */
    @GetMapping
    fun compare(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double
    ): ResponseEntity<CompareResponse> {
        val filter = LcAggFilter()

        // Intersect 방식 (먼저 호출)
        val intersectStart = System.currentTimeMillis()
        val intersectResult = intersectQueryService.findByBboxIntersects(swLng, swLat, neLng, neLat, filter)
        val intersectElapsed = System.currentTimeMillis() - intersectStart

        // Center Contains 방식 (나중 호출)
        val centerStart = System.currentTimeMillis()
        val centerResult = geoQueryService.findByBbox(swLng, swLat, neLng, neLat, filter)
        val centerElapsed = System.currentTimeMillis() - centerStart

        // 교집합, 차집합 계산
        val centerPnus = centerResult.keys
        val intersectPnus = intersectResult.keys
        val commonPnus = centerPnus.intersect(intersectPnus)
        val onlyCenterPnus = centerPnus - intersectPnus
        val onlyIntersectPnus = intersectPnus - centerPnus

        // 시간 차이 계산
        val timeDiff = centerElapsed - intersectElapsed
        val faster = if (timeDiff > 0) "intersect" else if (timeDiff < 0) "center" else "same"

        return ResponseEntity.ok(CompareResponse(
            center = QueryResult(
                count = centerResult.size,
                elapsedMs = centerElapsed,
                queryType = "geo_bounding_box (center point)",
                items = centerResult.values.toList()
            ),
            intersect = QueryResult(
                count = intersectResult.size,
                elapsedMs = intersectElapsed,
                queryType = "geo_shape intersects (geometry)",
                items = intersectResult.values.toList()
            ),
            diff = DiffResult(
                common = commonPnus.size,
                onlyCenter = onlyCenterPnus.size,
                onlyIntersect = onlyIntersectPnus.size,
                onlyCenterSample = onlyCenterPnus.take(5).toList(),
                onlyIntersectSample = onlyIntersectPnus.take(5).toList(),
                commonItems = commonPnus.mapNotNull { centerResult[it] },
                onlyCenterItems = onlyCenterPnus.mapNotNull { centerResult[it] },
                onlyIntersectItems = onlyIntersectPnus.mapNotNull { intersectResult[it] }
            ),
            timeDiff = kotlin.math.abs(timeDiff),
            faster = faster
        ))
    }
}

data class CompareResponse(
    val center: QueryResult,
    val intersect: QueryResult,
    val diff: DiffResult,
    val timeDiff: Long,
    val faster: String  // "center", "intersect", "same"
)

data class QueryResult(
    val count: Int,
    val elapsedMs: Long,
    val queryType: String,
    val items: List<LandCompactGeoData>
)

data class DiffResult(
    val common: Int,
    val onlyCenter: Int,
    val onlyIntersect: Int,
    val onlyCenterSample: List<String>,
    val onlyIntersectSample: List<String>,
    val commonItems: List<LandCompactGeoData>,
    val onlyCenterItems: List<LandCompactGeoData>,
    val onlyIntersectItems: List<LandCompactGeoData>
)

/**
 * 비교 페이지
 */
@Controller
@RequestMapping("/page/es/markers-compare")
class MarkerComparePageController(
    @Value("\${naver.map.client-id}") private val naverMapClientId: String
) {
    @GetMapping
    fun page(model: Model): String {
        model.addAttribute("naverMapClientId", naverMapClientId)
        model.addAttribute("title", "Center vs Intersect Compare")
        return "es/markers-compare"
    }
}
