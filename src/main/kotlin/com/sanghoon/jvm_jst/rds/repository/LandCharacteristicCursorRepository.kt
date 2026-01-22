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
     * 시군구(SGG) 기반 PNU 경계 조회
     * SGG를 N개 그룹으로 나눠서 워커별로 분배
     */
    fun findPnuBoundaries(partitions: Int): List<String> {
        // 실제 존재하는 시군구 코드 조회 (정렬됨)
        val sggCodes = jdbcTemplate.queryForList("""
            SELECT DISTINCT LEFT(pnu, 5) as sgg
            FROM external_data.land_characteristic
            WHERE center IS NOT NULL
            ORDER BY sgg
        """.trimIndent(), String::class.java)

        if (sggCodes.isEmpty()) return listOf("0".repeat(19), "9".repeat(19))

        // SGG를 N개 그룹으로 나누기
        val groupSize = (sggCodes.size + partitions - 1) / partitions  // 올림 나눗셈
        val boundaries = mutableListOf<String>()

        for (i in 0 until partitions) {
            val idx = i * groupSize
            if (idx < sggCodes.size) {
                boundaries.add(sggCodes[idx].padEnd(19, '0'))
            }
        }
        boundaries.add("9".repeat(19))

        return boundaries
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
                    jiyuk_cd_1, jimok_cd, area, price,
                    ST_AsGeoJSON(geometry) as geometry_geojson
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
                    jiyuk_cd_1, jimok_cd, area, price,
                    ST_AsGeoJSON(geometry) as geometry_geojson
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
                price = rs.getString("price"),
                geometryGeoJson = rs.getString("geometry_geojson")
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
    val price: String?,
    val geometryGeoJson: String?
)
