package com.sanghoon.jvm_jst.pnu.repository

import com.sanghoon.jvm_jst.pnu.entity.PnuBoundaryRegion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PnuBoundaryRegionRepository : JpaRepository<PnuBoundaryRegion, String> {

    /**
     * region_code로 조회
     */
    fun findByRegionCode(regionCode: String): PnuBoundaryRegion?

    /**
     * 여러 region_code로 조회
     */
    @Query("SELECT b FROM PnuBoundaryRegion b WHERE b.regionCode IN :codes")
    fun findByRegionCodes(codes: Collection<String>): List<PnuBoundaryRegion>

    /**
     * region_code 앞자리로 계층 조회 (시도, 시군구, 읍면동)
     * ex) 1168010100 → ["11", "11680", "11680101"]
     */
    @Query("SELECT b FROM PnuBoundaryRegion b WHERE b.regionCode IN :codes ORDER BY LENGTH(b.regionCode)")
    fun findHierarchy(codes: Collection<String>): List<PnuBoundaryRegion>
}
