package com.sanghoon.jvm_jst.rds.repository

import com.sanghoon.jvm_jst.entity.BuildingLedgerOutline
import com.sanghoon.jvm_jst.entity.BuildingLedgerOutlineSummaries
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * 건축물대장 요약 Repository
 * PNU는 sigunguCd(5) + bjdongCd(5) + platGbCd(1) + bun(4) + ji(4) = 19자리로 구성
 */
@Repository
class BuildingLedgerOutlineSummariesRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    fun findByPnuIn(pnuList: Collection<String>): List<BuildingLedgerOutlineSummaries> {
        if (pnuList.isEmpty()) return emptyList()

        val placeholders = pnuList.joinToString(",") { "?" }
        val sql = """
            SELECT * FROM external_data.building_ledger_outline_summaries
            WHERE pnu IN ($placeholders)
        """.trimIndent()

        return jdbcTemplate.query(sql, pnuList.toTypedArray()) { rs, _ ->
            BuildingLedgerOutlineSummaries(
                mgmBldrgstPk = rs.getString("mgm_bldrgst_pk") ?: "",
                sigunguCd = rs.getString("sigungu_cd") ?: "",
                bjdongCd = rs.getString("bjdong_cd") ?: "",
                platGbCd = rs.getString("plat_gb_cd") ?: "",
                bun = rs.getString("bun") ?: "",
                ji = rs.getString("ji") ?: "",
                mainPurpsCdNm = rs.getString("main_purps_cd_nm"),
                regstrGbCdNm = rs.getString("regstr_gb_cd_nm"),
                pmsDay = rs.getString("pms_day"),
                stcnsDay = rs.getString("stcns_day"),
                useAprDay = rs.getString("use_apr_day"),
                totArea = rs.getString("tot_area"),
                platArea = rs.getString("plat_area"),
                archArea = rs.getString("arch_area")
            )
        }
    }
}

/**
 * 건축물대장 상세 Repository
 */
@Repository
class BuildingLedgerOutlineRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    fun findByPnuIn(pnuList: Collection<String>): List<BuildingLedgerOutline> {
        if (pnuList.isEmpty()) return emptyList()

        val placeholders = pnuList.joinToString(",") { "?" }
        val sql = """
            SELECT * FROM external_data.building_ledger_outline
            WHERE pnu IN ($placeholders)
        """.trimIndent()

        return jdbcTemplate.query(sql, pnuList.toTypedArray()) { rs, _ ->
            BuildingLedgerOutline(
                mgmBldrgstPk = rs.getString("mgm_bldrgst_pk") ?: "",
                sigunguCd = rs.getString("sigungu_cd") ?: "",
                bjdongCd = rs.getString("bjdong_cd") ?: "",
                platGbCd = rs.getString("plat_gb_cd") ?: "",
                bun = rs.getString("bun") ?: "",
                ji = rs.getString("ji") ?: "",
                mainPurpsCdNm = rs.getString("main_purps_cd_nm"),
                regstrGbCdNm = rs.getString("regstr_gb_cd_nm"),
                pmsDay = rs.getString("pms_day"),
                stcnsDay = rs.getString("stcns_day"),
                useAprDay = rs.getString("use_apr_day"),
                totArea = rs.getString("tot_area"),
                platArea = rs.getString("plat_area"),
                archArea = rs.getString("arch_area")
            )
        }
    }
}
