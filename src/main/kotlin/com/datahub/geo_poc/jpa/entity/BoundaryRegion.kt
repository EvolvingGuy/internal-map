package com.datahub.geo_poc.jpa.entity

import jakarta.persistence.*
import org.locationtech.jts.geom.Geometry

@Entity
@Table(name = "boundary_region", schema = "mart_data")
data class BoundaryRegion(
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
    val geom: Geometry? = null,

    @Column(name = "is_donut_polygon")
    val isDonutPolygon: Boolean? = null,

    @Column(name = "center_geom", columnDefinition = "geometry")
    val centerGeom: Geometry? = null,

    @Column(name = "center_lng")
    val centerLng: Double? = null,

    @Column(name = "center_lat")
    val centerLat: Double? = null,

    @Column(name = "area_paths", columnDefinition = "jsonb")
    val areaPaths: String? = null,

    @Column(name = "gubun")
    val gubun: String? = null
)
