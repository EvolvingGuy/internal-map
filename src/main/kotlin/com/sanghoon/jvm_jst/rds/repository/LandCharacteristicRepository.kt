package com.sanghoon.jvm_jst.rds.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * 토지특성 조회 결과 (Native Query용)
 */
data class LandCharacteristicRow(
    val pnu: String,
    val geometryWkb: ByteArray,   // WKB 바이너리
    val centerWkb: ByteArray,     // WKB 바이너리
    val area: Double?,
    val isDonut: Boolean?
)

/**
 * 토지특성 Repository (Native Query)
 * geometry를 WKB로 조회하여 JTS 변환용
 */
@Repository
class LandCharacteristicRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    /**
     * PNU 목록으로 토지특성 조회
     * geometry, center를 WKB 바이너리로 반환
     */
    fun findByPnus(pnus: Collection<String>): List<LandCharacteristicRow> {
        if (pnus.isEmpty()) return emptyList()

        val placeholders = pnus.joinToString(",") { "?" }
        val sql = """
            SELECT
                pnu,
                ST_AsBinary(geometry) as geometry_wkb,
                ST_AsBinary(center) as center_wkb,
                CAST(NULLIF(area, '') AS DOUBLE PRECISION) as area,
                is_donut
            FROM external_data.land_characteristic
            WHERE pnu IN ($placeholders)
              AND geometry IS NOT NULL
              AND center IS NOT NULL
        """.trimIndent()

        return jdbcTemplate.query(sql, pnus.toTypedArray()) { rs, _ ->
            LandCharacteristicRow(
                pnu = rs.getString("pnu"),
                geometryWkb = rs.getBytes("geometry_wkb"),
                centerWkb = rs.getBytes("center_wkb"),
                area = rs.getObject("area") as? Double,
                isDonut = rs.getObject("is_donut") as? Boolean
            )
        }
    }
}
