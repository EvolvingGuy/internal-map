package com.sanghoon.jvm_jst.pnu.entity

import jakarta.persistence.*
import java.io.Serializable

// ========================
// 복합키 ID 클래스
// ========================

data class PnuAggId(
    val code: Long = 0,
    val h3Index: Long = 0
) : Serializable

data class PnuAggStaticRegionId(
    val level: String = "",
    val code: Long = 0
) : Serializable

// ========================
// EMD 엔티티 (읍면동)
// ========================

@Entity
@Table(name = "r3_pnu_agg_emd_11", schema = "manage")
@IdClass(PnuAggId::class)
class PnuAggEmd11(
    @Id
    @Column(name = "code")
    val code: Long = 0,

    @Id
    @Column(name = "h3_index")
    val h3Index: Long = 0,

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)

@Entity
@Table(name = "r3_pnu_agg_emd_10", schema = "manage")
@IdClass(PnuAggId::class)
class PnuAggEmd10(
    @Id
    @Column(name = "code")
    val code: Long = 0,

    @Id
    @Column(name = "h3_index")
    val h3Index: Long = 0,

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)

@Entity
@Table(name = "r3_pnu_agg_emd_09", schema = "manage")
@IdClass(PnuAggId::class)
class PnuAggEmd09(
    @Id
    @Column(name = "code")
    val code: Long = 0,

    @Id
    @Column(name = "h3_index")
    val h3Index: Long = 0,

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)

// ========================
// SGG 엔티티 (시군구)
// ========================

@Entity
@Table(name = "r3_pnu_agg_sgg_08", schema = "manage")
@IdClass(PnuAggId::class)
class PnuAggSgg08(
    @Id
    @Column(name = "code")
    val code: Long = 0,

    @Id
    @Column(name = "h3_index")
    val h3Index: Long = 0,

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)

@Entity
@Table(name = "r3_pnu_agg_sgg_07", schema = "manage")
@IdClass(PnuAggId::class)
class PnuAggSgg07(
    @Id
    @Column(name = "code")
    val code: Long = 0,

    @Id
    @Column(name = "h3_index")
    val h3Index: Long = 0,

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)

// ========================
// SD 엔티티 (시도)
// ========================

@Entity
@Table(name = "r3_pnu_agg_sd_06", schema = "manage")
@IdClass(PnuAggId::class)
class PnuAggSd06(
    @Id
    @Column(name = "code")
    val code: Long = 0,

    @Id
    @Column(name = "h3_index")
    val h3Index: Long = 0,

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)

@Entity
@Table(name = "r3_pnu_agg_sd_05", schema = "manage")
@IdClass(PnuAggId::class)
class PnuAggSd05(
    @Id
    @Column(name = "code")
    val code: Long = 0,

    @Id
    @Column(name = "h3_index")
    val h3Index: Long = 0,

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)

// ========================
// Static Region 엔티티 (고정형)
// ========================

@Entity
@Table(name = "r3_pnu_agg_static_region", schema = "manage")
@IdClass(PnuAggStaticRegionId::class)
class PnuAggStaticRegion(
    @Id
    @Column(name = "level")
    val level: String = "",

    @Id
    @Column(name = "code")
    val code: Long = 0,

    @Column(name = "name")
    val name: String = "",

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "center_lat")
    val centerLat: Double = 0.0,

    @Column(name = "center_lng")
    val centerLng: Double = 0.0
)
