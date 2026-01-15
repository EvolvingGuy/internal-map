package com.sanghoon.jvm_jst.legacy.region

import jakarta.persistence.*

@Entity
@Table(name = "boundary_region", schema = "mart_data")
class BoundaryRegionEntity(
    @Id
    @Column(name = "region_code")
    val regionCode: String = "",

    @Column(name = "region_korean_name")
    val regionKoreanName: String? = null,

    @Column(name = "gubun")
    val gubun: String? = null
)