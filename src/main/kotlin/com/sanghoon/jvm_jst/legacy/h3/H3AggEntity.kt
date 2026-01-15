package com.sanghoon.jvm_jst.legacy.h3

import jakarta.persistence.*
import java.io.Serializable

// 복합키 클래스
data class H3AggEmdId(
    val bjdongCd: String = "",
    val h3Index: String = ""
) : Serializable

data class H3AggSggId(
    val sggCd: String = "",
    val h3Index: String = ""
) : Serializable

data class H3AggSdId(
    val sdCd: String = "",
    val h3Index: String = ""
) : Serializable

// 읍면동 (res 11)
@Entity
@Table(name = "r3_pnu_agg_emd", schema = "manage")
@IdClass(H3AggEmdId::class)
class H3AggEmdEntity(
    @Id
    @Column(name = "bjdong_cd")
    val bjdongCd: String = "",

    @Id
    @Column(name = "h3_index")
    val h3Index: String = "",

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)

// 시군구 (res 9)
@Entity
@Table(name = "r3_pnu_agg_sgg", schema = "manage")
@IdClass(H3AggSggId::class)
class H3AggSggEntity(
    @Id
    @Column(name = "sgg_cd")
    val sggCd: String = "",

    @Id
    @Column(name = "h3_index")
    val h3Index: String = "",

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)

// 시도 (res 6)
@Entity
@Table(name = "r3_pnu_agg_sd", schema = "manage")
@IdClass(H3AggSdId::class)
class H3AggSdEntity(
    @Id
    @Column(name = "sd_cd")
    val sdCd: String = "",

    @Id
    @Column(name = "h3_index")
    val h3Index: String = "",

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)

// 읍면동 res 10 (~66m)
data class H3AggEmd10Id(
    val bjdongCd: String = "",
    val h3Index: String = ""
) : Serializable
