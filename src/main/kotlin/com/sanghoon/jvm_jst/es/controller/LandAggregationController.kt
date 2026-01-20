package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.document.LandAggregationDocument
import com.sanghoon.jvm_jst.es.service.LandAggregationService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 토지 어그리게이션 API 컨트롤러
 * - 인덱싱 API
 * - 조회 API
 * 현재 미사용 - 필요 시 @RestController 활성화
 */
// @RestController
// @RequestMapping("/api/es/land")
class LandAggregationController(
    private val service: LandAggregationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ==================== 인덱싱 API ====================

    /**
     * 전체 재인덱싱
     * POST /api/es/land/reindex
     */
    @PostMapping("/reindex")
    fun reindexAll(): ResponseEntity<ReindexResponse> {
        log.info("[reindexAll] 요청")
        val startTime = System.currentTimeMillis()
        service.reindexAll()
        val elapsed = System.currentTimeMillis() - startTime
        return ResponseEntity.ok(ReindexResponse(success = true, elapsedMs = elapsed))
    }

    /**
     * 특정 지역 재인덱싱
     * POST /api/es/land/reindex/{bjdongCd}
     */
    @PostMapping("/reindex/{bjdongCd}")
    fun reindexByRegion(@PathVariable bjdongCd: String): ResponseEntity<ReindexResponse> {
        log.info("[reindexByRegion] bjdongCd={}", bjdongCd)
        val startTime = System.currentTimeMillis()
        service.reindexByRegion(bjdongCd)
        val elapsed = System.currentTimeMillis() - startTime
        return ResponseEntity.ok(ReindexResponse(success = true, elapsedMs = elapsed))
    }

    /**
     * 단건 인덱싱
     * POST /api/es/land/index
     */
    @PostMapping("/index")
    fun index(@RequestBody document: LandAggregationDocument): ResponseEntity<LandAggregationDocument> {
        log.info("[index] pnu={}", document.pnu)
        val saved = service.index(document)
        return ResponseEntity.ok(saved)
    }

    /**
     * 벌크 인덱싱
     * POST /api/es/land/index/bulk
     */
    @PostMapping("/index/bulk")
    fun indexBulk(@RequestBody documents: List<LandAggregationDocument>): ResponseEntity<BulkIndexResponse> {
        log.info("[indexBulk] size={}", documents.size)
        val startTime = System.currentTimeMillis()
        val saved = service.indexBulk(documents)
        val elapsed = System.currentTimeMillis() - startTime
        return ResponseEntity.ok(BulkIndexResponse(count = saved.size, elapsedMs = elapsed))
    }

    // ==================== 조회 API ====================

    /**
     * bbox 범위 내 토지 조회
     * GET /api/es/land/bbox?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/bbox")
    fun findByBbox(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double
    ): ResponseEntity<BboxQueryResponse> {
        log.info("[findByBbox] sw=({}, {}), ne=({}, {})", swLng, swLat, neLng, neLat)
        val startTime = System.currentTimeMillis()
        val results = service.findByBbox(swLng, swLat, neLng, neLat)
        val elapsed = System.currentTimeMillis() - startTime
        return ResponseEntity.ok(BboxQueryResponse(count = results.size, data = results, elapsedMs = elapsed))
    }

    /**
     * bbox 범위 내 카운트
     * GET /api/es/land/bbox/count?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/bbox/count")
    fun countByBbox(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double
    ): ResponseEntity<CountResponse> {
        log.info("[countByBbox] sw=({}, {}), ne=({}, {})", swLng, swLat, neLng, neLat)
        val startTime = System.currentTimeMillis()
        val count = service.countByBbox(swLng, swLat, neLng, neLat)
        val elapsed = System.currentTimeMillis() - startTime
        return ResponseEntity.ok(CountResponse(count = count, elapsedMs = elapsed))
    }

    /**
     * 법정동코드로 조회
     * GET /api/es/land/region/{bjdongCd}
     */
    @GetMapping("/region/{bjdongCd}")
    fun findByRegion(@PathVariable bjdongCd: String): ResponseEntity<BboxQueryResponse> {
        log.info("[findByRegion] bjdongCd={}", bjdongCd)
        val startTime = System.currentTimeMillis()
        val results = service.findByBjdongCd(bjdongCd)
        val elapsed = System.currentTimeMillis() - startTime
        return ResponseEntity.ok(BboxQueryResponse(count = results.size, data = results, elapsedMs = elapsed))
    }

    /**
     * PNU로 단건 조회
     * GET /api/es/land/{pnu}
     */
    @GetMapping("/{pnu}")
    fun findByPnu(@PathVariable pnu: String): ResponseEntity<LandAggregationDocument> {
        log.info("[findByPnu] pnu={}", pnu)
        val result = service.findByPnu(pnu)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    /**
     * bbox 범위 내 지역별 그룹핑
     * GET /api/es/land/bbox/group?swLng=...&swLat=...&neLng=...&neLat=...
     */
    @GetMapping("/bbox/group")
    fun groupByRegion(
        @RequestParam swLng: Double,
        @RequestParam swLat: Double,
        @RequestParam neLng: Double,
        @RequestParam neLat: Double
    ): ResponseEntity<GroupQueryResponse> {
        log.info("[groupByRegion] sw=({}, {}), ne=({}, {})", swLng, swLat, neLng, neLat)
        val startTime = System.currentTimeMillis()
        val grouped = service.groupByRegion(swLng, swLat, neLng, neLat)
        val elapsed = System.currentTimeMillis() - startTime
        return ResponseEntity.ok(GroupQueryResponse(regionCount = grouped.size, data = grouped, elapsedMs = elapsed))
    }
}

// ==================== Response DTOs ====================

data class ReindexResponse(
    val success: Boolean,
    val elapsedMs: Long
)

data class BulkIndexResponse(
    val count: Int,
    val elapsedMs: Long
)

data class BboxQueryResponse(
    val count: Int,
    val data: List<LandAggregationDocument>,
    val elapsedMs: Long
)

data class CountResponse(
    val count: Long,
    val elapsedMs: Long
)

data class GroupQueryResponse(
    val regionCount: Int,
    val data: Map<String, List<LandAggregationDocument>>,
    val elapsedMs: Long
)
