package com.sanghoon.jvm_jst.es.service

import com.sanghoon.jvm_jst.es.cache.BoundaryRegionSpatialCache
import com.sanghoon.jvm_jst.es.cache.RegionLevel
import com.sanghoon.jvm_jst.es.document.LandAggregationDocument
import com.sanghoon.jvm_jst.es.repository.LandAggregationRepository
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Service

/**
 * 토지 어그리게이션 서비스
 * - ES 인덱싱
 * - 지역코드 기반 1차 탐색 + JVM 좌표 공간 탐색 2차 처리
 * 현재 미사용 - 필요 시 @Service 활성화
 */
// @Service
class LandAggregationService(
    private val repository: LandAggregationRepository,
    private val elasticsearchOperations: ElasticsearchOperations,
    private val spatialCache: BoundaryRegionSpatialCache
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ==================== 인덱싱 ====================

    /**
     * 단건 인덱싱
     */
    fun index(document: LandAggregationDocument): LandAggregationDocument {
        log.debug("[index] pnu={}", document.pnu)
        return repository.save(document)
    }

    /**
     * 벌크 인덱싱
     */
    fun indexBulk(documents: List<LandAggregationDocument>): List<LandAggregationDocument> {
        log.info("[indexBulk] size={}", documents.size)
        return repository.saveAll(documents).toList()
    }

    /**
     * DB에서 조회하여 ES 인덱싱 (전체)
     * TODO: 실제 구현 시 land_master 조회 로직 추가
     */
    fun reindexAll() {
        log.info("[reindexAll] 전체 재인덱싱 시작")
        // TODO: land_master 테이블에서 배치로 조회하여 인덱싱
        // 1. land_master 테이블 조회 (페이징/커서 기반)
        // 2. LandAggregationDocument 변환
        // 3. 벌크 인덱싱
        log.info("[reindexAll] 전체 재인덱싱 완료")
    }

    /**
     * 특정 지역 재인덱싱
     * TODO: 실제 구현 시 land_master 조회 로직 추가
     */
    fun reindexByRegion(bjdongCd: String) {
        log.info("[reindexByRegion] bjdongCd={}", bjdongCd)
        // TODO: 특정 법정동 코드에 해당하는 데이터 재인덱싱
    }

    // ==================== 조회 ====================

    /**
     * bbox 범위 내 토지 조회
     * 1. bbox → 교차하는 지역코드 목록 (JVM 공간 탐색)
     * 2. 지역코드로 ES 1차 조회
     * 3. JVM에서 좌표 기반 2차 필터링
     */
    fun findByBbox(swLng: Double, swLat: Double, neLng: Double, neLat: Double): List<LandAggregationDocument> {
        val startTime = System.currentTimeMillis()

        // 1. bbox와 교차하는 읍면동 지역 조회
        val intersectingRegions = spatialCache.findIntersecting(swLng, swLat, neLng, neLat, RegionLevel.DONG)
        val regionCodes = intersectingRegions.map { it.regionCode }

        if (regionCodes.isEmpty()) {
            log.debug("[findByBbox] 교차하는 지역 없음")
            return emptyList()
        }

        log.debug("[findByBbox] 교차 지역 수: {}", regionCodes.size)

        // 2. ES에서 지역코드로 1차 조회
        val candidates = repository.findByBjdongCdIn(regionCodes)

        // 3. JVM에서 좌표 기반 2차 필터링
        val filtered = candidates.filter { doc ->
            val lat = doc.centerLat ?: return@filter false
            val lng = doc.centerLng ?: return@filter false
            lng in swLng..neLng && lat in swLat..neLat
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.debug("[findByBbox] candidates={}, filtered={}, elapsed={}ms", candidates.size, filtered.size, elapsed)

        return filtered
    }

    /**
     * 법정동코드로 조회
     */
    fun findByBjdongCd(bjdongCd: String): List<LandAggregationDocument> {
        log.debug("[findByBjdongCd] bjdongCd={}", bjdongCd)
        return repository.findByBjdongCd(bjdongCd)
    }

    /**
     * PNU로 단건 조회
     */
    fun findByPnu(pnu: String): LandAggregationDocument? {
        log.debug("[findByPnu] pnu={}", pnu)
        return repository.findById(pnu).orElse(null)
    }

    /**
     * bbox 범위 내 어그리게이션 (카운트)
     * TODO: ES aggregation 쿼리로 개선 가능
     */
    fun countByBbox(swLng: Double, swLat: Double, neLng: Double, neLat: Double): Long {
        val results = findByBbox(swLng, swLat, neLng, neLat)
        return results.size.toLong()
    }

    /**
     * bbox 범위 내 지역별 그룹핑
     */
    fun groupByRegion(swLng: Double, swLat: Double, neLng: Double, neLat: Double): Map<String, List<LandAggregationDocument>> {
        val results = findByBbox(swLng, swLat, neLng, neLat)
        return results.groupBy { it.bjdongCd ?: "unknown" }
    }
}
