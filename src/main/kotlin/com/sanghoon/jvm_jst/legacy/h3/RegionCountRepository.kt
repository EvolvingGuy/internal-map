package com.sanghoon.jvm_jst.legacy.h3

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RegionCountRepository : JpaRepository<RegionCountEntity, RegionCountId> {

    @Query("""
        SELECT region_code, cnt, center_lat, center_lng
        FROM manage.r3_pnu_region_count
        WHERE region_level = :level AND region_code IN :codes
    """, nativeQuery = true)
    fun findByLevelAndCodes(level: String, codes: Collection<String>): List<Array<Any>>
}
