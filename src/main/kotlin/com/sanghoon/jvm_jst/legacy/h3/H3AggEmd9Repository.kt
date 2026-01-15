package com.sanghoon.jvm_jst.legacy.h3

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface H3AggEmd9Repository : JpaRepository<H3AggEmd9Entity, H3AggEmd9Id> {

    @Query("""
        SELECT bjdong_cd, h3_index, cnt, sum_lat, sum_lng
        FROM manage.r3_pnu_agg_emd_9
        WHERE h3_index IN :h3Indexes
    """, nativeQuery = true)
    fun findByH3IndexesWithBjdong(h3Indexes: Collection<String>): List<Array<Any>>

    @Query("""
        SELECT h3_index, SUM(cnt), SUM(sum_lat), SUM(sum_lng)
        FROM manage.r3_pnu_agg_emd_9
        WHERE h3_index IN :h3Indexes
        GROUP BY h3_index
    """, nativeQuery = true)
    fun findByH3Indexes(h3Indexes: Collection<String>): List<Array<Any>>

    @Query("""
        SELECT bjdong_cd, h3_index, cnt, sum_lat, sum_lng
        FROM manage.r3_pnu_agg_emd_9
        WHERE bjdong_cd LIKE :sidoCode || '%'
    """, nativeQuery = true)
    fun findBySidoCode(sidoCode: String): List<Array<Any>>

    @Query("""
        SELECT bjdong_cd, h3_index, cnt, sum_lat, sum_lng
        FROM manage.r3_pnu_agg_emd_9
    """, nativeQuery = true)
    fun findAll9(): List<Array<Any>>
}
