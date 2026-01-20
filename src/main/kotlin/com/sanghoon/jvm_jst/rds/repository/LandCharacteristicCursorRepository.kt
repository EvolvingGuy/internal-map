package com.sanghoon.jvm_jst.rds.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * 토지특성 커서 조회용 Row
 */
data class LandCharacteristicCursorRow(
    val pnu: String,
    val centerLat: Double,
    val centerLng: Double
)

/**
 * 토지특성 커서 기반 Repository (LSRC/LC 인덱싱용)
 */
@Repository
class LandCharacteristicCursorRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    /**
     * 전체 필지 수 조회
     */
    fun countAll(): Long {
        val sql = "SELECT COUNT(*) FROM external_data.land_characteristic WHERE center IS NOT NULL"
        return jdbcTemplate.queryForObject(sql, Long::class.java) ?: 0
    }

    /**
     * PNU 범위로 커서 기반 조회 (LSRC 인덱싱용)
     * @param minPnu 시작 PNU (inclusive)
     * @param maxPnu 종료 PNU (exclusive)
     * @param lastPnu 마지막 조회 PNU (커서)
     * @param limit 조회 건수
     */
    fun findByPnuRangeAfter(
        minPnu: String,
        maxPnu: String,
        lastPnu: String?,
        limit: Int
    ): List<LandCharacteristicCursorRow> {
        val sql = if (lastPnu == null) {
            """
                SELECT pnu, ST_Y(center) as center_lat, ST_X(center) as center_lng
                FROM external_data.land_characteristic
                WHERE pnu >= ? AND pnu < ? AND center IS NOT NULL
                ORDER BY pnu
                LIMIT ?
            """.trimIndent()
        } else {
            """
                SELECT pnu, ST_Y(center) as center_lat, ST_X(center) as center_lng
                FROM external_data.land_characteristic
                WHERE pnu >= ? AND pnu < ? AND pnu > ? AND center IS NOT NULL
                ORDER BY pnu
                LIMIT ?
            """.trimIndent()
        }

        val params = if (lastPnu == null) {
            arrayOf(minPnu, maxPnu, limit)
        } else {
            arrayOf(minPnu, maxPnu, lastPnu, limit)
        }

        return jdbcTemplate.query(sql, params) { rs, _ ->
            LandCharacteristicCursorRow(
                pnu = rs.getString("pnu"),
                centerLat = rs.getDouble("center_lat"),
                centerLng = rs.getDouble("center_lng")
            )
        }
    }

    /**
     * PNU 범위 경계 조회 (N등분용)
     * @param partitions 분할 수
     * @return PNU 경계 목록 (partitions + 1 개)
     */
    fun findPnuBoundaries(partitions: Int): List<String> {
        val sql = """
            WITH numbered AS (
                SELECT pnu, ROW_NUMBER() OVER (ORDER BY pnu) as rn, COUNT(*) OVER() as total
                FROM external_data.land_characteristic
                WHERE center IS NOT NULL
            )
            SELECT pnu FROM numbered
            WHERE rn = 1
               OR rn = total
               OR rn % (total / ?) = 0
            ORDER BY pnu
        """.trimIndent()

        val boundaries = jdbcTemplate.queryForList(sql, String::class.java, partitions)

        // 마지막 경계를 최대값으로 설정 (exclusive용)
        return if (boundaries.isNotEmpty()) {
            boundaries.dropLast(1) + listOf("9999999999999999999")
        } else {
            listOf("0000000000000000000", "9999999999999999999")
        }
    }

    /**
     * LC 인덱싱용 커서 기반 조회 (모든 필드 포함)
     */
    fun findForLcIndexing(
        minPnu: String,
        maxPnu: String,
        lastPnu: String?,
        limit: Int
    ): List<LandCharacteristicLcRow> {
        val sql = if (lastPnu == null) {
            """
                SELECT
                    pnu, bjdong_cd,
                    ST_Y(center) as center_lat, ST_X(center) as center_lng,
                    jiyuk_cd_1, jimok_cd, area, price
                FROM external_data.land_characteristic
                WHERE pnu >= ? AND pnu < ? AND center IS NOT NULL
                ORDER BY pnu
                LIMIT ?
            """.trimIndent()
        } else {
            """
                SELECT
                    pnu, bjdong_cd,
                    ST_Y(center) as center_lat, ST_X(center) as center_lng,
                    jiyuk_cd_1, jimok_cd, area, price
                FROM external_data.land_characteristic
                WHERE pnu >= ? AND pnu < ? AND pnu > ? AND center IS NOT NULL
                ORDER BY pnu
                LIMIT ?
            """.trimIndent()
        }

        val params = if (lastPnu == null) {
            arrayOf(minPnu, maxPnu, limit)
        } else {
            arrayOf(minPnu, maxPnu, lastPnu, limit)
        }

        return jdbcTemplate.query(sql, params) { rs, _ ->
            LandCharacteristicLcRow(
                pnu = rs.getString("pnu"),
                bjdongCd = rs.getString("bjdong_cd"),
                centerLat = rs.getDouble("center_lat"),
                centerLng = rs.getDouble("center_lng"),
                jiyukCd1 = rs.getString("jiyuk_cd_1"),
                jimokCd = rs.getString("jimok_cd"),
                area = rs.getString("area"),
                price = rs.getString("price")
            )
        }
    }
}

/**
 * LC 인덱싱용 Row
 */
data class LandCharacteristicLcRow(
    val pnu: String,
    val bjdongCd: String?,
    val centerLat: Double,
    val centerLng: Double,
    val jiyukCd1: String?,
    val jimokCd: String?,
    val area: String?,
    val price: String?
)
