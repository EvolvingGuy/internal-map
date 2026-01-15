package com.sanghoon.jvm_jst.pnu.repository

import com.sanghoon.jvm_jst.pnu.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

// ========================
// EMD 레포지토리 (읍면동)
// ========================

@Repository
interface PnuAggEmd11Repository : JpaRepository<PnuAggEmd11, PnuAggId> {
    @Query("SELECT p FROM PnuAggEmd11 p WHERE p.h3Index IN :h3Indexes")
    fun findByH3Indexes(h3Indexes: Collection<Long>): List<PnuAggEmd11>
}

@Repository
interface PnuAggEmd10Repository : JpaRepository<PnuAggEmd10, PnuAggId> {
    @Query("SELECT p FROM PnuAggEmd10 p WHERE p.h3Index IN :h3Indexes")
    fun findByH3Indexes(h3Indexes: Collection<Long>): List<PnuAggEmd10>
}

@Repository
interface PnuAggEmd09Repository : JpaRepository<PnuAggEmd09, PnuAggId> {
    @Query("SELECT p FROM PnuAggEmd09 p WHERE p.h3Index IN :h3Indexes")
    fun findByH3Indexes(h3Indexes: Collection<Long>): List<PnuAggEmd09>
}

// ========================
// SGG 레포지토리 (시군구)
// ========================

@Repository
interface PnuAggSgg08Repository : JpaRepository<PnuAggSgg08, PnuAggId> {
    @Query("SELECT p FROM PnuAggSgg08 p WHERE p.h3Index IN :h3Indexes")
    fun findByH3Indexes(h3Indexes: Collection<Long>): List<PnuAggSgg08>
}

@Repository
interface PnuAggSgg07Repository : JpaRepository<PnuAggSgg07, PnuAggId> {
    @Query("SELECT p FROM PnuAggSgg07 p WHERE p.h3Index IN :h3Indexes")
    fun findByH3Indexes(h3Indexes: Collection<Long>): List<PnuAggSgg07>
}

// ========================
// SD 레포지토리 (시도)
// ========================

@Repository
interface PnuAggSd06Repository : JpaRepository<PnuAggSd06, PnuAggId> {
    @Query("SELECT p FROM PnuAggSd06 p WHERE p.h3Index IN :h3Indexes")
    fun findByH3Indexes(h3Indexes: Collection<Long>): List<PnuAggSd06>
}

@Repository
interface PnuAggSd05Repository : JpaRepository<PnuAggSd05, PnuAggId> {
    @Query("SELECT p FROM PnuAggSd05 p WHERE p.h3Index IN :h3Indexes")
    fun findByH3Indexes(h3Indexes: Collection<Long>): List<PnuAggSd05>
}

// ========================
// Static Region 레포지토리 (고정형)
// ========================

@Repository
interface PnuAggStaticRegionRepository : JpaRepository<PnuAggStaticRegion, PnuAggStaticRegionId> {
    @Query("SELECT p FROM PnuAggStaticRegion p WHERE p.level = :level AND p.code IN :codes")
    fun findByLevelAndCodes(level: String, codes: Collection<Long>): List<PnuAggStaticRegion>
}
