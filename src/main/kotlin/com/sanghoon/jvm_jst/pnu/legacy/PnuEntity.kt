package com.sanghoon.jvm_jst.pnu.legacy

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "land_characteristic", schema = "external_data")
class PnuEntity(
    @Id
    @Column(name = "pnu")
    val pnu: String,

    @Column(name = "bjdong_cd")
    val regionCode: String
) {
    // center는 PostGIS geometry라 native query로 처리
    @Transient
    var centerLng: Double = 0.0

    @Transient
    var centerLat: Double = 0.0
}
