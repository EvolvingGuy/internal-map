package com.sanghoon.jvm_jst.rds.entity

import jakarta.persistence.*

/**
 * 행정구역 경계 엔티티 (PNU용)
 * 테이블: mart_data.boundary_region
 */
@Entity
@Table(name = "boundary_region", schema = "mart_data")
class PnuBoundaryRegion(
    @Id
    @Column(name = "region_code")
    val regionCode: String,

    @Column(name = "region_english_name")
    val regionEnglishName: String? = null,

    @Column(name = "region_korean_name")
    val regionKoreanName: String? = null,

    @Column(name = "region_full_korean_name")
    val regionFullKoreanName: String? = null,

    @Column(name = "geom", columnDefinition = "geometry")
    val geom: String? = null,

    @Column(name = "is_donut_polygon")
    val isDonutPolygon: Boolean? = null,

    @Column(name = "center_geom", columnDefinition = "geometry")
    val centerGeom: String? = null,

    @Column(name = "center_lng")
    val centerLng: Double? = null,

    @Column(name = "center_lat")
    val centerLat: Double? = null,

    @Column(name = "area_paths", columnDefinition = "jsonb")
    val areaPaths: String? = null,

    @Column(name = "gubun")
    val gubun: String? = null
)
