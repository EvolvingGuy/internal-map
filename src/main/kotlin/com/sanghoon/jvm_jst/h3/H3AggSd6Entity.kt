package com.sanghoon.jvm_jst.h3

import jakarta.persistence.*
import java.io.Serializable

data class H3AggSd6Id(
    val sdCd: String = "",
    val h3Index: String = ""
) : Serializable

@Entity
@Table(name = "r3_pnu_agg_sd_6", schema = "manage")
@IdClass(H3AggSd6Id::class)
class H3AggSd6Entity(
    @Id @Column(name = "sd_cd") val sdCd: String = "",
    @Id @Column(name = "h3_index") val h3Index: String = "",
    val cnt: Int = 0,
    @Column(name = "sum_lat") val sumLat: Double = 0.0,
    @Column(name = "sum_lng") val sumLng: Double = 0.0
)
