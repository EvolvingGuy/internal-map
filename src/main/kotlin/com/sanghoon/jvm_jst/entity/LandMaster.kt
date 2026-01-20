package com.sanghoon.jvm_jst.entity

import jakarta.persistence.*
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * land_master 테이블
 * - land_characteristic 기반 마스터 테이블
 * - 타입 변경: bjdong_cd(BIGINT), area(DOUBLE), price(DOUBLE), crtn_day(DATE)
 * - 이후 다른 테이블 조인하여 컬럼 추가 예정
 */
@Entity
@Table(name = "land_master", schema = "manage")
data class LandMaster(
    @Id
    @Column(name = "pnu")
    val pnu: String,

    @Column(name = "bjdong_cd")
    val bjdongCd: Long? = null,

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
    val area: Double? = null,

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
    val price: Double? = null,

    @Column(name = "crtn_day")
    val crtnDay: LocalDate? = null,

    @Column(name = "geometry", columnDefinition = "geometry(Geometry, 4326)")
    val geometry: Geometry? = null,

    @Column(name = "center", columnDefinition = "geometry(Point, 4326)")
    val center: Point? = null,

    @Column(name = "center_lat")
    val centerLat: Double? = null,

    @Column(name = "center_lng")
    val centerLng: Double? = null,

    @Column(name = "is_donut")
    val isDonut: Boolean? = null,

    @Column(name = "create_dt")
    val createDt: LocalDateTime? = null,

    @Column(name = "update_dt")
    val updateDt: LocalDateTime? = null
)
