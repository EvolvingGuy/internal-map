package com.sanghoon.jvm_jst.es.service

import com.sanghoon.jvm_jst.es.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Marker API 서비스
 * land_compact + registration 조합
 */
@Service
class MarkerService(
    private val lcQueryService: LandCompactQueryService,
    private val regAggService: RegistrationAggService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Type 1: Land 우선 조회
     * 1. bbox 내 land_compact 조회 (필터 적용)
     * 2. 조회된 pnu로 registration aggregation
     * 3. 교집합 응답
     */
    fun getMarkersType1(request: MarkerRequest): MarkerResponse {
        val startTime = System.currentTimeMillis()

        // 1. land_compact 조회
        val lcDataMap = lcQueryService.findByBbox(
            swLng = request.swLng,
            swLat = request.swLat,
            neLng = request.neLng,
            neLat = request.neLat,
            filter = request.lcFilter
        )

        if (lcDataMap.isEmpty()) {
            return MarkerResponse(
                totalCount = 0,
                elapsedMs = System.currentTimeMillis() - startTime,
                items = emptyList()
            )
        }

        // 2. registration aggregation
        val regAggMap = regAggService.aggregateByPnuIds(
            pnuIds = lcDataMap.keys.toList(),
            userId = request.userId,
            minCreatedDate = request.minCreatedDate,
            maxCreatedDate = request.maxCreatedDate
        )

        // 3. 교집합 (registration이 있는 pnu만)
        val items = lcDataMap.entries
            .filter { regAggMap.containsKey(it.key) }
            .map { (pnu, lcData) ->
                MarkerItem(
                    pnu = pnu,
                    center = lcData.center,
                    land = lcData.land,
                    building = lcData.building,
                    lastRealEstateTrade = lcData.trade,
                    registration = regAggMap[pnu]!!
                )
            }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[Marker Type1] lc={}, reg={}, intersection={}, elapsed={}ms",
            lcDataMap.size, regAggMap.size, items.size, elapsed)

        return MarkerResponse(
            totalCount = items.size,
            elapsedMs = elapsed,
            items = items
        )
    }

    /**
     * Type 2: Registration 우선 조회
     * 1. bbox 내 registration 조회 -> pnu_id set
     * 2. pnu_id로 land_compact 조회
     * 3. 교집합 응답
     */
    fun getMarkersType2(request: MarkerRequest): MarkerResponse {
        val startTime = System.currentTimeMillis()

        // 1. registration에서 pnu_id set 조회
        val regPnuIds = regAggService.findPnuIdsByBbox(
            swLng = request.swLng,
            swLat = request.swLat,
            neLng = request.neLng,
            neLat = request.neLat,
            minCreatedDate = request.minCreatedDate,
            maxCreatedDate = request.maxCreatedDate
        )

        if (regPnuIds.isEmpty()) {
            return MarkerResponse(
                totalCount = 0,
                elapsedMs = System.currentTimeMillis() - startTime,
                items = emptyList()
            )
        }

        // 2. land_compact 조회 (pnu 목록으로)
        val lcDataMap = lcQueryService.findByPnuIds(regPnuIds)

        // 3. registration aggregation (land_compact에 있는 pnu만)
        val commonPnuIds = regPnuIds.intersect(lcDataMap.keys)

        if (commonPnuIds.isEmpty()) {
            return MarkerResponse(
                totalCount = 0,
                elapsedMs = System.currentTimeMillis() - startTime,
                items = emptyList()
            )
        }

        val regAggMap = regAggService.aggregateByPnuIds(
            pnuIds = commonPnuIds.toList(),
            userId = request.userId,
            minCreatedDate = request.minCreatedDate,
            maxCreatedDate = request.maxCreatedDate
        )

        // 4. 교집합 머지
        val items = commonPnuIds.mapNotNull { pnu ->
            val lcData = lcDataMap[pnu] ?: return@mapNotNull null
            val regAgg = regAggMap[pnu] ?: return@mapNotNull null
            MarkerItem(
                pnu = pnu,
                center = lcData.center,
                land = lcData.land,
                building = lcData.building,
                lastRealEstateTrade = lcData.trade,
                registration = regAgg
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[Marker Type2] regPnu={}, lc={}, intersection={}, elapsed={}ms",
            regPnuIds.size, lcDataMap.size, items.size, elapsed)

        return MarkerResponse(
            totalCount = items.size,
            elapsedMs = elapsed,
            items = items
        )
    }
}
