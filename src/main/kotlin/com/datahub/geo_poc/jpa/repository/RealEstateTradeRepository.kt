package com.datahub.geo_poc.jpa.repository

import com.datahub.geo_poc.jpa.entity.RealEstateTrade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 실거래 Repository (JPA)
 * external_data.r3_real_estate_trade 테이블 사용
 */
interface RealEstateTradeRepository : JpaRepository<RealEstateTrade, Long> {

    /**
     * PNU 목록으로 최신 실거래 조회 (pnu별 max(id) 한 건)
     * PostgreSQL LATERAL JOIN으로 각 pnu별 최신(id DESC) 1건 조회
     * string_to_array 사용 (ROW expression 1664 제한 회피)
     */
    @Query(
        value = "SELECT t.* FROM unnest(string_to_array(:pnuCsv, ',')) AS p(pnu) CROSS JOIN LATERAL (SELECT * FROM external_data.r3_real_estate_trade WHERE pnu = p.pnu ORDER BY id DESC LIMIT 1) t",
        nativeQuery = true
    )
    fun findLatestByPnuIn(@Param("pnuCsv") pnuCsv: String): List<RealEstateTrade>

    /**
     * PNU 목록으로 전체 실거래 조회 (pnu별 N건)
     * string_to_array 사용 (ROW expression 1664 제한 회피)
     */
    @Query(
        value = "SELECT t.* FROM external_data.r3_real_estate_trade t WHERE t.pnu = ANY(string_to_array(:pnuCsv, ',')) ORDER BY t.pnu, t.contract_date DESC",
        nativeQuery = true
    )
    fun findAllByPnuIn(@Param("pnuCsv") pnuCsv: String): List<RealEstateTrade>
}
