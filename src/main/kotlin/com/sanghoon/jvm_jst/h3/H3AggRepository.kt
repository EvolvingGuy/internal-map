package com.sanghoon.jvm_jst.h3

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface H3AggEmdRepository : JpaRepository<H3AggEmdEntity, H3AggEmdId> {

    @Query("""
        SELECT h3_index, cnt, sum_lat, sum_lng, bjdong_cd
        FROM manage.r3_pnu_agg_emd_10
        WHERE bjdong_cd IN :regionCodes
    """, nativeQuery = true)
    fun findByRegionCodes(regionCodes: Collection<String>): List<Array<Any>>

    @Query("""
        SELECT h3_index, SUM(cnt), SUM(sum_lat), SUM(sum_lng)
        FROM manage.r3_pnu_agg_emd_10
        WHERE h3_index IN :h3Indexes
        GROUP BY h3_index
    """, nativeQuery = true)
    fun findByH3Indexes(h3Indexes: Collection<String>): List<Array<Any>>

    @Query("""
        SELECT bjdong_cd, h3_index, cnt, sum_lat, sum_lng
        FROM manage.r3_pnu_agg_emd_10
        WHERE h3_index IN :h3Indexes
    """, nativeQuery = true)
    fun findByH3IndexesWithBjdong(h3Indexes: Collection<String>): List<Array<Any>>

    @Query("""
        SELECT bjdong_cd, h3_index, cnt, sum_lat, sum_lng
        FROM manage.r3_pnu_agg_emd_10
        WHERE bjdong_cd >= :minCode AND bjdong_cd < :maxCode
    """, nativeQuery = true)
    fun findBySidoCodeRange(minCode: Int, maxCode: Int): List<Array<Any>>
}

interface H3AggSggRepository : JpaRepository<H3AggSggEntity, H3AggSggId> {

    @Query("""
        SELECT h3_index, cnt, sum_lat, sum_lng, sgg_cd
        FROM manage.r3_pnu_agg_sgg
        WHERE sgg_cd IN :sggCodes
    """, nativeQuery = true)
    fun findBySggCodes(sggCodes: Collection<String>): List<Array<Any>>

    @Query("""
        SELECT h3_index, SUM(cnt), SUM(sum_lat), SUM(sum_lng)
        FROM manage.r3_pnu_agg_sgg
        WHERE h3_index IN :h3Indexes
        GROUP BY h3_index
    """, nativeQuery = true)
    fun findByH3Indexes(h3Indexes: Collection<String>): List<Array<Any>>
}

interface H3AggSdRepository : JpaRepository<H3AggSdEntity, H3AggSdId> {

    @Query("""
        SELECT h3_index, cnt, sum_lat, sum_lng, sd_cd
        FROM manage.r3_pnu_agg_sd
        WHERE sd_cd IN :sdCodes
    """, nativeQuery = true)
    fun findBySdCodes(sdCodes: Collection<String>): List<Array<Any>>

    @Query("""
        SELECT h3_index, SUM(cnt), SUM(sum_lat), SUM(sum_lng)
        FROM manage.r3_pnu_agg_sd
        WHERE h3_index IN :h3Indexes
        GROUP BY h3_index
    """, nativeQuery = true)
    fun findByH3Indexes(h3Indexes: Collection<String>): List<Array<Any>>
}
