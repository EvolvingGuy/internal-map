package com.datahub.geo_poc.jpa.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "r3_real_estate_trade", schema = "external_data")
data class RealEstateTrade(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "jibun_address", nullable = false)
    val jibunAddress: String,

    @Column(name = "road_address")
    val roadAddress: String? = null,

    @Column(name = "pnu", nullable = false)
    val pnu: String,

    @Column(name = "bjd_code", nullable = false)
    val bjdCode: String,

    @Column(name = "building_name")
    val buildingName: String? = null,

    @Column(name = "property", nullable = false)
    val property: String,

    @Column(name = "is_multi_unit")
    val isMultiUnit: Boolean? = null,

    @Column(name = "rent_type", nullable = false)
    val rentType: String,

    @Column(name = "contract_date", nullable = false)
    val contractDate: LocalDate,

    @Column(name = "contract_year", nullable = false, length = 4)
    val contractYear: String,

    @Column(name = "contract_ym", nullable = false, length = 7)
    val contractYm: String,

    @Column(name = "effective_amount", nullable = false)
    val effectiveAmount: Long,

    @Column(name = "monthly_rent_amount")
    val monthlyRentAmount: Long? = null,

    @Column(name = "building_amount_per_nla_m2", precision = 20, scale = 2)
    val buildingAmountPerNlaM2: BigDecimal? = null,

    @Column(name = "building_amount_per_gla_m2", precision = 20, scale = 2)
    val buildingAmountPerGlaM2: BigDecimal? = null,

    @Column(name = "land_amount_per_m2", precision = 20, scale = 2)
    val landAmountPerM2: BigDecimal? = null,

    @Column(name = "nla_m2", precision = 20, scale = 2)
    val nlaM2: BigDecimal? = null,

    @Column(name = "gla_m2", precision = 20, scale = 2)
    val glaM2: BigDecimal? = null,

    @Column(name = "land_m2", precision = 20, scale = 2)
    val landM2: BigDecimal? = null,

    @Column(name = "pla_m2", precision = 20, scale = 2)
    val plaM2: BigDecimal? = null,

    @Column(name = "land_use_right_m2", precision = 20, scale = 2)
    val landUseRightM2: BigDecimal? = null,

    @Column(name = "is_shared", nullable = false)
    val isShared: Boolean = false,

    @Column(name = "construction_year")
    val constructionYear: Int? = null,

    @Column(name = "floor")
    val floor: String? = null,

    @Column(name = "buyer")
    val buyer: String? = null,

    @Column(name = "seller")
    val seller: String? = null,

    @Column(name = "contract_type")
    val contractType: String? = null,

    @Column(name = "building_structure")
    val buildingStructure: String? = null,

    @Column(name = "land_use")
    val landUse: String? = null,

    @Column(name = "contract_status")
    val contractStatus: String? = null,

    @Column(name = "contract_period")
    val contractPeriod: String? = null,

    @Column(name = "registration_date")
    val registrationDate: LocalDate? = null,

    @Column(name = "unit")
    val unit: String? = null,

    @Column(name = "contract_area", precision = 20, scale = 2)
    val contractArea: BigDecimal? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by", nullable = false)
    val createdBy: Int = 999999998,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    @Column(name = "updated_by")
    val updatedBy: Int? = null,

    @Column(name = "dwelling_type")
    val dwellingType: String? = null,

    @Column(name = "zonning")
    val zonning: String? = null,

    @Column(name = "bld_use_or_land_category")
    val bldUseOrLandCategory: String? = null
)
