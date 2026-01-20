package com.sanghoon.jvm_jst.rds.repository

import com.sanghoon.jvm_jst.rds.entity.PnuH310
import com.sanghoon.jvm_jst.rds.entity.PnuH310Id
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PnuH310Repository : JpaRepository<PnuH310, PnuH310Id> {
    @Query("SELECT p FROM PnuH310 p WHERE p.h3Index IN :h3Indexes")
    fun findByH3Indexes(h3Indexes: Collection<Long>): List<PnuH310>
}
