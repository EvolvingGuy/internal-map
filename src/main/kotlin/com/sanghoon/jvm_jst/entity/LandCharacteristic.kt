package com.sanghoon.jvm_jst.entity

import jakarta.persistence.*
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import java.time.LocalDateTime

@Entity
@Table(name = "land_characteristic", schema = "external_data")
data class LandCharacteristic(
    @Id
    @Column(name = "pnu")
    val pnu: String,

    @Column(name = "bjdong_cd")
    val bjdongCd: String? = null,

    @Column(name = "bjdong_nm")
    val bjdongNm: String? = null,

    @Column(name = "regstr_gb_cd")
    val regstrGbCd: String? = null,

    @Column(name = "regstr_gb")
    val regstrGb: String? = null,

    @Column(name = "jibun")
    val jibun: String? = null,

    @Column(name = "jimok_sign")
    val jimokSign: String? = null,

    @Column(name = "std_year")
    val stdYear: String? = null,

    @Column(name = "std_month")
    val stdMonth: String? = null,

    @Column(name = "jimok_cd")
    val jimokCd: String? = null,

    @Column(name = "jimok")
    val jimok: String? = null,

    @Column(name = "area")
    val area: String? = null,

    @Column(name = "jiyuk_cd_1")
    val jiyukCd1: String? = null,

    @Column(name = "jiyuk_1")
    val jiyuk1: String? = null,

    @Column(name = "jiyuk_cd_2")
    val jiyukCd2: String? = null,

    @Column(name = "jiyuk_2")
    val jiyuk2: String? = null,

    @Column(name = "land_use_cd")
    val landUseCd: String? = null,

    @Column(name = "land_use")
    val landUse: String? = null,

    @Column(name = "height_cd")
    val heightCd: String? = null,

    @Column(name = "height")
    val height: String? = null,

    @Column(name = "shape_cd")
    val shapeCd: String? = null,

    @Column(name = "shape")
    val shape: String? = null,

    @Column(name = "road_cd")
    val roadCd: String? = null,

    @Column(name = "road")
    val road: String? = null,

    @Column(name = "price")
    val price: String? = null,

    @Column(name = "crtn_day")
    val crtnDay: String? = null,

    @Column(name = "geometry", columnDefinition = "geometry(Geometry, 4326)")
    val geometry: Geometry? = null,

    @Column(name = "create_dt")
    val createDt: LocalDateTime? = null,

    @Column(name = "center", columnDefinition = "geometry(Point, 4326)")
    val center: Point? = null,

    @Column(name = "is_donut")
    val isDonut: Boolean? = null
)
