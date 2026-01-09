package com.sanghoon.jvm_jst.h3

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface H3AggSd6Repository : JpaRepository<H3AggSd6Entity, H3AggSd6Id> {

    @Query("""
        SELECT sd_cd, h3_index, cnt, sum_lat, sum_lng
        FROM manage.r3_pnu_agg_sd_6
        WHERE h3_index IN :h3Indexes
    """, nativeQuery = true)
    fun findByH3IndexesWithSd(h3Indexes: Collection<String>): List<Array<Any>>

    @Query("""
        SELECT sd_cd, h3_index, cnt, sum_lat, sum_lng
        FROM manage.r3_pnu_agg_sd_6
        WHERE sd_cd = :sidoCode
    """, nativeQuery = true)
    fun findBySidoCode(sidoCode: String): List<Array<Any>>
}
