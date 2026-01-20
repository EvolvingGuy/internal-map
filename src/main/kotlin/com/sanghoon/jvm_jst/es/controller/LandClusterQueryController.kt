package com.sanghoon.jvm_jst.es.controller

import com.sanghoon.jvm_jst.es.document.LandClusterDocument
import com.sanghoon.jvm_jst.es.dto.ClusterType
import com.sanghoon.jvm_jst.es.dto.LandClusterRequest
import com.sanghoon.jvm_jst.es.dto.LandClusterResponse
import com.sanghoon.jvm_jst.es.service.LandClusterQueryService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/es/land-cluster")
class LandClusterQueryController(
    private val queryService: LandClusterQueryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 클러스터 조회
     * GET /api/es/land-cluster/clusters?southwestLatitude=...&clusterType=GRID&jimokCd=01,02
     */
    @GetMapping("/clusters")
    fun getClusters(
        @RequestParam southwestLatitude: Double,
        @RequestParam southwestLongitude: Double,
        @RequestParam northeastLatitude: Double,
        @RequestParam northeastLongitude: Double,
        @RequestParam clusterType: ClusterType,
        @RequestParam(required = false) jimokCd: Set<String>?,
        @RequestParam(required = false) jiyukCd1: Set<String>?,
        @RequestParam(required = false) mainPurpsCd: Set<String>?
    ): ResponseEntity<LandClusterResponse> {
        val request = LandClusterRequest(
            southwestLatitude = southwestLatitude,
            southwestLongitude = southwestLongitude,
            northeastLatitude = northeastLatitude,
            northeastLongitude = northeastLongitude,
            clusterType = clusterType,
            jimokCd = jimokCd ?: emptySet(),
            jiyukCd1 = jiyukCd1 ?: emptySet(),
            mainPurpsCd = mainPurpsCd ?: emptySet()
        )

        log.info("[getClusters] type={}, bbox=({},{})~({},{}), filters={}",
            clusterType, request.swLat, request.swLng, request.neLat, request.neLng, request.hasFilters())

        val startTime = System.currentTimeMillis()
        val response = queryService.getClusters(request)
        val elapsed = System.currentTimeMillis() - startTime

        return ResponseEntity.ok(response.copy(elapsedMs = elapsed))
    }

    /**
     * PNU로 단건 조회
     * GET /api/es/land-cluster/{pnu}
     */
    @GetMapping("/{pnu}")
    fun findByPnu(@PathVariable pnu: String): ResponseEntity<LandClusterDocument> {
        log.debug("[findByPnu] pnu={}", pnu)
        val result = queryService.findByPnu(pnu)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    /**
     * 인덱스 총 카운트
     * GET /api/es/land-cluster/count
     */
    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        val total = queryService.count()
        return ResponseEntity.ok(mapOf("total" to total))
    }
}
