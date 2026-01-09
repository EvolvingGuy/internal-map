package com.sanghoon.jvm_jst.h3

import jakarta.persistence.*
import java.io.Serializable

data class RegionCountId(
    val regionLevel: String = "",
    val regionCode: String = ""
) : Serializable

@Entity
@Table(name = "r3_pnu_region_count", schema = "manage")
@IdClass(RegionCountId::class)
class RegionCountEntity(
    @Id
    @Column(name = "region_level")
    val regionLevel: String = "",

    @Id
    @Column(name = "region_code")
    val regionCode: String = "",

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "center_lat")
    val centerLat: Double = 0.0,

    @Column(name = "center_lng")
    val centerLng: Double = 0.0
)
