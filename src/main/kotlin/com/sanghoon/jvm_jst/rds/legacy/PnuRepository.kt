package com.sanghoon.jvm_jst.rds.legacy

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PnuRepository : JpaRepository<PnuEntity, String> {

    // 읍면동리 코드 기준 PNU 리스팅 (center 좌표 포함)
    @Query("""
        SELECT pnu, bjdong_cd, ST_X(center) as lng, ST_Y(center) as lat
        FROM external_data.land_characteristic
        WHERE bjdong_cd = :regionCode
    """, nativeQuery = true)
    fun findByRegionCodeNative(regionCode: String): List<Array<Any>>

    // 여러 읍면동리 코드로 PNU 리스팅 (IN절)
    @Query("""
        SELECT pnu, bjdong_cd, ST_X(center) as lng, ST_Y(center) as lat
        FROM external_data.land_characteristic
        WHERE bjdong_cd IN :regionCodes
    """, nativeQuery = true)
    fun findByRegionCodesNative(regionCodes: Collection<String>): List<Array<Any>>

    // 읍면동리 기준 GROUP BY COUNT
    @Query("""
        SELECT bjdong_cd, COUNT(*)
        FROM external_data.land_characteristic
        GROUP BY bjdong_cd
    """, nativeQuery = true)
    fun countByRegionCodeGrouped(): List<Array<Any>>

    // 좌표로 필지 조회 (ST_CONTAINS)
    @Query("""
        SELECT pnu, bjdong_cd, bjdong_nm, jibun, regstr_gb
        FROM external_data.land_characteristic
        WHERE ST_CONTAINS(geometry, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
    """, nativeQuery = true)
    fun findByPosition(lng: Double, lat: Double): List<Array<Any>>
}
