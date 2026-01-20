package com.datahub.geo_poc.jpa.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "r3_pnu_agg_emd_10",
    schema = "manage",
    uniqueConstraints = [UniqueConstraint(columnNames = ["code", "h3_index"])]
)
class PnuAggEmd10(
    @Id
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "code")
    val code: Long = 0,

    @Column(name = "h3_index")
    val h3Index: Long = 0,

    @Column(name = "cnt")
    val cnt: Int = 0,

    @Column(name = "sum_lat")
    val sumLat: Double = 0.0,

    @Column(name = "sum_lng")
    val sumLng: Double = 0.0
)