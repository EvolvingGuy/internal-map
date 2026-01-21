package com.sanghoon.jvm_jst.rds.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 실거래 프로젝션 DTO (LC 인덱싱용)
 */
data class RealEstateTradeProjection(
    val pnu: String,
    val property: String,
    val contractDate: LocalDate,
    val effectiveAmount: Long,
    val buildingAmountPerM2: BigDecimal?,
    val landAmountPerM2: BigDecimal?
)

/**
 * 실거래 Repository (Native Query)
 * manage.r3_real_estate_trade 테이블 사용
 */
@Repository
class RealEstateTradeRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    /**
     * PNU 목록으로 최신 실거래 조회 (pnu별 max(id) 한 건)
     * LATERAL JOIN으로 각 pnu마다 인덱스 스캔
     */
    fun findLatestByPnuIn(pnuList: Collection<String>): List<RealEstateTradeProjection> {
        if (pnuList.isEmpty()) return emptyList()

        val sql = """
            SELECT t.*
            FROM unnest(?::text[]) AS p(pnu)
            CROSS JOIN LATERAL (
                SELECT pnu,
                       property,
                       contract_date,
                       effective_amount,
                       building_amount_per_nla_m2 as building_amount_per_m2,
                       land_amount_per_m2
                FROM external_data.r3_real_estate_trade
                WHERE pnu = p.pnu
                ORDER BY id DESC
                LIMIT 1
            ) t
        """.trimIndent()

        val pnuArray = pnuList.toTypedArray()
        return jdbcTemplate.query(sql, arrayOf(pnuArray)) { rs, _ ->
            RealEstateTradeProjection(
                pnu = rs.getString("pnu"),
                property = rs.getString("property"),
                contractDate = rs.getDate("contract_date").toLocalDate(),
                effectiveAmount = rs.getLong("effective_amount"),
                buildingAmountPerM2 = rs.getBigDecimal("building_amount_per_m2"),
                landAmountPerM2 = rs.getBigDecimal("land_amount_per_m2")
            )
        }
    }
}
