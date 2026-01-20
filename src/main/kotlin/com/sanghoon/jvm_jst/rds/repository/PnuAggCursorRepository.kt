package com.sanghoon.jvm_jst.rds.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * PnuAgg EMD 10 커서 조회용 Row
 */
data class PnuAggEmd10Row(
    val id: Long,
    val code: Long,
    val h3Index: Long,
    val cnt: Int,
    val sumLat: Double,
    val sumLng: Double
)

/**
 * PnuAgg 커서 기반 Repository (LDRC 인덱싱용)
 */
@Repository
class PnuAggCursorRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    /**
     * EMD 10 최대 id 조회
     */
    fun maxIdEmd10(): Long {
        val sql = "SELECT COALESCE(MAX(id), 0) FROM manage.r3_pnu_agg_emd_10"
        return jdbcTemplate.queryForObject(sql, Long::class.java) ?: 0
    }

    /**
     * id 범위로 커서 기반 조회
     * @param minId 시작 id (exclusive, 첫 조회시 0)
     * @param maxId 종료 id (inclusive)
     * @param limit 조회 건수
     */
    fun findEmd10ByIdRange(minId: Long, maxId: Long, limit: Int): List<PnuAggEmd10Row> {
        val sql = """
            SELECT id, code, h3_index, cnt, sum_lat, sum_lng
            FROM manage.r3_pnu_agg_emd_10
            WHERE id > ? AND id <= ?
            ORDER BY id
            LIMIT ?
        """.trimIndent()

        return jdbcTemplate.query(sql, arrayOf(minId, maxId, limit)) { rs, _ ->
            PnuAggEmd10Row(
                id = rs.getLong("id"),
                code = rs.getLong("code"),
                h3Index = rs.getLong("h3_index"),
                cnt = rs.getInt("cnt"),
                sumLat = rs.getDouble("sum_lat"),
                sumLng = rs.getDouble("sum_lng")
            )
        }
    }

    /**
     * EMD 10에서 유니크 SGG 코드 목록 추출 (앞 5자리)
     */
    fun findDistinctSggCodes(): List<Long> {
        val sql = "SELECT DISTINCT code / 100000 * 100000 as sgg_code FROM manage.r3_pnu_agg_emd_10 ORDER BY sgg_code"
        return jdbcTemplate.query(sql) { rs, _ -> rs.getLong("sgg_code") }
    }

    /**
     * EMD 10에서 유니크 SD 코드 목록 추출 (앞 2자리)
     */
    fun findDistinctSdCodes(): List<Long> {
        val sql = "SELECT DISTINCT code / 100000000 * 100000000 as sd_code FROM manage.r3_pnu_agg_emd_10 ORDER BY sd_code"
        return jdbcTemplate.query(sql) { rs, _ -> rs.getLong("sd_code") }
    }
}
