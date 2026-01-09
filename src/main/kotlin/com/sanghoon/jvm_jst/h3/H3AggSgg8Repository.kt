package com.sanghoon.jvm_jst.h3

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface H3AggSgg8Repository : JpaRepository<H3AggSgg8Entity, H3AggSgg8Id> {

    @Query("""
        SELECT sgg_cd, h3_index, cnt, sum_lat, sum_lng
        FROM manage.r3_pnu_agg_sgg_8
        WHERE h3_index IN :h3Indexes
    """, nativeQuery = true)
    fun findByH3IndexesWithSgg(h3Indexes: Collection<String>): List<Array<Any>>

    @Query("""
        SELECT sgg_cd, h3_index, cnt, sum_lat, sum_lng
        FROM manage.r3_pnu_agg_sgg_8
        WHERE sgg_cd LIKE :sidoCode || '%'
    """, nativeQuery = true)
    fun findBySidoCode(sidoCode: String): List<Array<Any>>
}
