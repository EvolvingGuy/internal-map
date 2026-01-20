package com.sanghoon.jvm_jst.rds.entity

import jakarta.persistence.*
import java.io.Serializable

/**
 * PNU-H3 매핑 복합키
 */
data class PnuH310Id(
    val h3Index: Long = 0,
    val pnu: String = ""
) : Serializable

/**
 * PNU-H3 매핑 엔티티 (r3_pnu_h3_10)
 * H3 index → PNU set 매핑용
 */
@Entity
@Table(name = "r3_pnu_h3_10", schema = "manage")
@IdClass(PnuH310Id::class)
class PnuH310(
    @Id
    @Column(name = "h3_index")
    val h3Index: Long = 0,

    @Id
    @Column(name = "pnu")
    val pnu: String = ""
)
