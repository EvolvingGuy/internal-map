package com.sanghoon.jvm_jst.es.service

import com.sanghoon.jvm_jst.es.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Marker Geo API 서비스
 * land_compact_geo (geometry를 object로 저장) + registration 조합
 */
@Service
class MarkerGeoService(
    private val lcGeoQueryService: LandCompactGeoQueryService,
    private val regAggService: RegistrationAggService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Type 1: Land 우선 조회
     */
    fun getMarkersType1(request: MarkerRequest): MarkerGeoResponse {
        val startTime = System.currentTimeMillis()
        val hasRegFilter = request.minCreatedDate != null || request.maxCreatedDate != null

        val lcDataMap = lcGeoQueryService.findByBbox(
            swLng = request.swLng,
            swLat = request.swLat,
            neLng = request.neLng,
            neLat = request.neLat,
            filter = request.lcFilter
        )

        if (lcDataMap.isEmpty()) {
            return MarkerGeoResponse(
                totalCount = 0,
                elapsedMs = System.currentTimeMillis() - startTime,
                items = emptyList()
            )
        }

        val regAggMap = regAggService.aggregateByPnuIds(
            pnuIds = lcDataMap.keys.toList(),
            userId = request.userId,
            minCreatedDate = request.minCreatedDate,
            maxCreatedDate = request.maxCreatedDate
        )

        val items = if (hasRegFilter) {
            lcDataMap.entries
                .filter { regAggMap.containsKey(it.key) }
                .map { (pnu, lcData) ->
                    MarkerGeoItem(
                        pnu = pnu,
                        center = lcData.center,
                        land = lcData.land,
                        building = lcData.building,
                        lastRealEstateTrade = lcData.trade,
                        registration = regAggMap[pnu]!!
                    )
                }
        } else {
            lcDataMap.entries.map { (pnu, lcData) ->
                MarkerGeoItem(
                    pnu = pnu,
                    center = lcData.center,
                    land = lcData.land,
                    building = lcData.building,
                    lastRealEstateTrade = lcData.trade,
                    registration = regAggMap[pnu]
                )
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[MarkerGeo Type1] lc={}, reg={}, result={}, elapsed={}ms",
            lcDataMap.size, regAggMap.size, items.size, elapsed)

        return MarkerGeoResponse(
            totalCount = items.size,
            elapsedMs = elapsed,
            items = items
        )
    }

    /**
     * Type 2: Registration 우선 조회
     */
    fun getMarkersType2(request: MarkerRequest): MarkerGeoResponse {
        val startTime = System.currentTimeMillis()
        val hasRegFilter = request.minCreatedDate != null || request.maxCreatedDate != null

        if (!hasRegFilter) {
            return getMarkersType2NoRegFilter(request, startTime)
        }

        val regPnuIds = regAggService.findPnuIdsByBbox(
            swLng = request.swLng,
            swLat = request.swLat,
            neLng = request.neLng,
            neLat = request.neLat,
            minCreatedDate = request.minCreatedDate,
            maxCreatedDate = request.maxCreatedDate
        )

        if (regPnuIds.isEmpty()) {
            return MarkerGeoResponse(
                totalCount = 0,
                elapsedMs = System.currentTimeMillis() - startTime,
                items = emptyList()
            )
        }

        val lcDataMap = lcGeoQueryService.findByPnuIds(regPnuIds, request.lcFilter)
        val commonPnuIds = regPnuIds.intersect(lcDataMap.keys)

        if (commonPnuIds.isEmpty()) {
            return MarkerGeoResponse(
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

        val items = commonPnuIds.mapNotNull { pnu ->
            val lcData = lcDataMap[pnu] ?: return@mapNotNull null
            val regAgg = regAggMap[pnu] ?: return@mapNotNull null
            MarkerGeoItem(
                pnu = pnu,
                center = lcData.center,
                land = lcData.land,
                building = lcData.building,
                lastRealEstateTrade = lcData.trade,
                registration = regAgg
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[MarkerGeo Type2] regPnu={}, lc={}, result={}, elapsed={}ms",
            regPnuIds.size, lcDataMap.size, items.size, elapsed)

        return MarkerGeoResponse(
            totalCount = items.size,
            elapsedMs = elapsed,
            items = items
        )
    }

    private fun getMarkersType2NoRegFilter(request: MarkerRequest, startTime: Long): MarkerGeoResponse {
        val lcDataMap = lcGeoQueryService.findByBbox(
            swLng = request.swLng,
            swLat = request.swLat,
            neLng = request.neLng,
            neLat = request.neLat,
            filter = request.lcFilter
        )

        if (lcDataMap.isEmpty()) {
            return MarkerGeoResponse(
                totalCount = 0,
                elapsedMs = System.currentTimeMillis() - startTime,
                items = emptyList()
            )
        }

        val regAggMap = regAggService.aggregateByPnuIds(
            pnuIds = lcDataMap.keys.toList(),
            userId = request.userId,
            minCreatedDate = null,
            maxCreatedDate = null
        )

        val items = lcDataMap.entries.map { (pnu, lcData) ->
            MarkerGeoItem(
                pnu = pnu,
                center = lcData.center,
                land = lcData.land,
                building = lcData.building,
                lastRealEstateTrade = lcData.trade,
                registration = regAggMap[pnu]
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[MarkerGeo Type2] lc={}, reg={}, result={}, elapsed={}ms",
            lcDataMap.size, regAggMap.size, items.size, elapsed)

        return MarkerGeoResponse(
            totalCount = items.size,
            elapsedMs = elapsed,
            items = items
        )
    }
}
