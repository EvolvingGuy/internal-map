package com.datahub.geo_poc.jpa.entity

import jakarta.persistence.*
import org.locationtech.jts.geom.Geometry
import java.time.LocalDateTime

@Entity
@Table(name = "r3_registration", schema = "manage")
data class Registration(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Int,

    @Column(name = "origin_registration_id")
    val originRegistrationId: Int? = null,

    @Column(name = "real_estate_number", nullable = false, length = 16)
    val realEstateNumber: String,

    @Column(name = "registration_type", length = 50)
    val registrationType: String? = null,

    @Column(name = "address", length = 500)
    val address: String? = null,

    @Column(name = "road_address", length = 500)
    val roadAddress: String? = null,

    @Column(name = "address_detail", length = 500)
    val addressDetail: String? = null,

    @Column(name = "pnu_id", length = 19)
    val pnuId: String? = null,

    @Column(name = "region_code", length = 10)
    val regionCode: String? = null,

    @Column(name = "sido_code", length = 2)
    val sidoCode: String? = null,

    @Column(name = "sgg_code", length = 3)
    val sggCode: String? = null,

    @Column(name = "umd_code", length = 3)
    val umdCode: String? = null,

    @Column(name = "ri_code", length = 2)
    val riCode: String? = null,

    @Column(name = "registration_process", nullable = false, length = 50)
    val registrationProcess: String,

    @Column(name = "completed_at")
    val completedAt: LocalDateTime? = null,

    @Column(name = "property", nullable = false, length = 50)
    val property: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    @Column(name = "updated_by")
    val updatedBy: Long? = null,

    @Column(name = "is_collateral_and_lease")
    val isCollateralAndLease: Boolean? = null,

    @Column(name = "is_sales_list")
    val isSalesList: Boolean? = null,

    @Column(name = "is_expunged")
    val isExpunged: Boolean? = null,

    @Column(name = "is_event_ignore")
    val isEventIgnore: Boolean? = null,

    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "is_reopen", nullable = false)
    val isReopen: Boolean,

    @Column(name = "geometry", columnDefinition = "geometry(Geometry, 4326)")
    val geometry: Geometry? = null,

    @Column(name = "transaction_number", length = 30)
    val transactionNumber: String? = null,

    @Column(name = "is_latest")
    val isLatest: Boolean? = null,

    @Column(name = "share_scope", length = 20)
    val shareScope: String? = null,

    @Column(name = "return_code", length = 20)
    val returnCode: String? = null,

    @Column(name = "refund_status", length = 20)
    val refundStatus: String? = null,

    @Column(name = "approval_number", length = 200)
    val approvalNumber: String? = null,

    @Column(name = "register_master_id")
    val registerMasterId: Long? = null,

    @Column(name = "permanent_document_type", length = 30)
    val permanentDocumentType: String? = null,

    @Column(name = "document_number")
    val documentNumber: String? = null
)
