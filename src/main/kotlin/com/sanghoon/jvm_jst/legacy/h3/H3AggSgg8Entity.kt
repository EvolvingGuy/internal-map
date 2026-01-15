package com.sanghoon.jvm_jst.legacy.h3

import jakarta.persistence.*
import java.io.Serializable

data class H3AggSgg8Id(
    val sggCd: String = "",
    val h3Index: String = ""
) : Serializable

@Entity
@Table(name = "r3_pnu_agg_sgg_8", schema = "manage")
@IdClass(H3AggSgg8Id::class)
class H3AggSgg8Entity(
    @Id @Column(name = "sgg_cd") val sggCd: String = "",
    @Id @Column(name = "h3_index") val h3Index: String = "",
    val cnt: Int = 0,
    @Column(name = "sum_lat") val sumLat: Double = 0.0,
    @Column(name = "sum_lng") val sumLng: Double = 0.0
)
