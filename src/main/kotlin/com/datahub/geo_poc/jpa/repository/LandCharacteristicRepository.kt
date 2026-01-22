package com.datahub.geo_poc.jpa.repository

import com.datahub.geo_poc.jpa.entity.LandCharacteristic
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import jakarta.persistence.QueryHint
import java.util.stream.Stream

private const val HINT_FETCH_SIZE = "org.hibernate.fetchSize"

/**
 * EMD 집계 결과 프로젝션
 */
interface EmdAggregationResult {
    val code: String
    val cnt: Long
    val sumLat: Double
    val sumLng: Double
}

/**
 * 토지특성 커서 기반 Repository (LSRC/LC 인덱싱용)
 */
interface LandCharacteristicRepository : JpaRepository<LandCharacteristic, String> {

    /**
     * 인덱싱 대상 필지 수 조회 (pnu, geometry, center 모두 NOT NULL)
     */
    @Query("SELECT COUNT(l) FROM LandCharacteristic l WHERE l.pnu IS NOT NULL AND l.geometry IS NOT NULL AND l.center IS NOT NULL")
    fun countIndexable(): Long

    /**
     * 시군구(SGG) 코드 목록 조회 (워커 분배용)
     * bjdong_cd 기준 - idx_lc_bjdong_cd_left5 인덱스 활용
     */
    @Query("SELECT DISTINCT FUNCTION('left', l.bjdongCd, 5) FROM LandCharacteristic l WHERE l.pnu IS NOT NULL AND l.geometry IS NOT NULL AND l.center IS NOT NULL ORDER BY FUNCTION('left', l.bjdongCd, 5)")
    fun findDistinctSggCodes(): List<String>

    /**
     * SGG 코드로 Stream 조회 (LC 인덱싱용)
     * bjdong_cd 기준 - idx_lc_bjdong_cd_left5 인덱스 활용
     * fetch size 5,000으로 메모리 효율적 처리
     */
    @QueryHints(QueryHint(name = HINT_FETCH_SIZE, value = "5000"))
    @Query("SELECT l FROM LandCharacteristic l WHERE FUNCTION('left', l.bjdongCd, 5) = :sggCode AND l.pnu IS NOT NULL AND l.geometry IS NOT NULL AND l.center IS NOT NULL ORDER BY l.pnu")
    fun streamBySggCode(@Param("sggCode") sggCode: String): Stream<LandCharacteristic>

    /**
     * EMD(읍면동) 코드 목록 조회 - bjdong_cd 기반
     */
    @Query("SELECT DISTINCT SUBSTRING(l.bjdongCd, 1, 8) FROM LandCharacteristic l WHERE l.center IS NOT NULL ORDER BY 1")
    fun findDistinctEmdCodes(): List<String>

    /**
     * 전체 EMD 집계 - bjdong_cd 기반 (한 방 쿼리)
     */
    @Query("""
        SELECT SUBSTRING(l.bjdongCd, 1, 8) as code,
               COUNT(l) as cnt,
               SUM(FUNCTION('ST_Y', l.center)) as sumLat,
               SUM(FUNCTION('ST_X', l.center)) as sumLng
        FROM LandCharacteristic l
        WHERE l.center IS NOT NULL
        GROUP BY SUBSTRING(l.bjdongCd, 1, 8)
    """)
    fun aggregateAllByEmd(): List<EmdAggregationResult>
}
