package com.sanghoon.jvm_jst.legacy.search

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "integrated_search_index", schema = "external_data")
class SearchEntity(
    @Id
    @Column(name = "pnu")
    val pnu: String,

    @Column(name = "bjdong_cd")
    val bjdongCd: String,

    @Column(name = "jibun_address")
    val jibunAddress: String?,

    @Column(name = "road_address")
    val roadAddress: String?,

    @Column(name = "building_name")
    val buildingName: String?,

    @Column(name = "lat")
    val lat: Double?,

    @Column(name = "lng")
    val lng: Double?,

    @Column(name = "search_text")
    val searchText: String?
)
