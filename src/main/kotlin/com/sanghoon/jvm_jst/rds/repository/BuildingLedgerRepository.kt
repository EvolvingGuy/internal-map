package com.sanghoon.jvm_jst.rds.repository

import com.sanghoon.jvm_jst.entity.BuildingLedgerOutline
import com.sanghoon.jvm_jst.entity.BuildingLedgerOutlineSummaries
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * 건축물대장 요약 Repository
 * PNU는 sigunguCd(5) + bjdongCd(5) + platGbCd(1) + bun(4) + ji(4) = 19자리로 구성
 */
@Repository
interface BuildingLedgerOutlineSummariesRepository : JpaRepository<BuildingLedgerOutlineSummaries, String> {

    /**
     * PNU 목록으로 건축물대장 요약 조회
     * land_characteristic PNU 11번째 자리 변환 필요: 1 -> 0, else -> 1
     */
    @Query("""
        SELECT b.* FROM external_data.building_ledger_outline_summaries b
        WHERE CONCAT(b.sigungu_cd, b.bjdong_cd, b.plat_gb_cd, b.bun, b.ji) IN :pnuList
    """, nativeQuery = true)
    fun findByPnuIn(pnuList: Collection<String>): List<BuildingLedgerOutlineSummaries>
}

/**
 * 건축물대장 상세 Repository
 */
@Repository
interface BuildingLedgerOutlineRepository : JpaRepository<BuildingLedgerOutline, String> {

    /**
     * PNU 목록으로 건축물대장 상세 조회
     * land_characteristic PNU 11번째 자리 변환 필요: 1 -> 0, else -> 1
     */
    @Query("""
        SELECT b.* FROM external_data.building_ledger_outline b
        WHERE CONCAT(b.sigungu_cd, b.bjdong_cd, b.plat_gb_cd, b.bun, b.ji) IN :pnuList
    """, nativeQuery = true)
    fun findByPnuIn(pnuList: Collection<String>): List<BuildingLedgerOutline>
}
