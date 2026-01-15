package com.sanghoon.jvm_jst.legacy.h3

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class H3AggEmd9Id(
    @Column(name = "bjdong_cd")
    val bjdongCd: String = "",

    @Column(name = "h3_index")
    val h3Index: String = ""
) : Serializable

@Entity
@Table(name = "r3_pnu_agg_emd_9", schema = "manage")
@IdClass(H3AggEmd9Id::class)
class H3AggEmd9Entity(
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
    val sumLng: Double = 0.0,

    @Column(name = "bjdong_name")
    val bjdongName: String? = null
)
