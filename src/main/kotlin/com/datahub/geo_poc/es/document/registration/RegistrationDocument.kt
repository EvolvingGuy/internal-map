package com.datahub.geo_poc.es.document.registration

import com.datahub.geo_poc.jpa.entity.Registration
import com.datahub.geo_poc.util.GeoJsonUtils
import org.locationtech.jts.geom.Point
import java.time.format.DateTimeFormatter

/**
 * Registration ES Document
 * 등기 데이터 인덱스
 */
data class RegistrationDocument(
    val id: Int,
    val originRegistrationId: Int?,
    val realEstateNumber: String,
    val registrationType: String?,
    val address: String?,
    val roadAddress: String?,
    val addressDetail: String?,
    val pnuId: String?,
    val regionCode: String?,
    val sidoCode: String?,
    val sggCode: String?,
    val umdCode: String?,
    val riCode: String?,
    val registrationProcess: String,
    val completedAt: String?,
    val property: String,
    val createdAt: String,
    val createdBy: Long,
    val updatedAt: String?,
    val updatedBy: Long?,
    val isCollateralAndLease: Boolean?,
    val isSalesList: Boolean?,
    val isExpunged: Boolean?,
    val isEventIgnore: Boolean?,
    val userId: Long?,
    val isReopen: Boolean,
    val geometry: Map<String, Any>?,
    val center: Map<String, Double>?,
    val transactionNumber: String?,
    val isLatest: Boolean?,
    val shareScope: String?,
    val returnCode: String?,
    val refundStatus: String?,
    val approvalNumber: String?,
    val registerMasterId: Long?,
    val permanentDocumentType: String?,
    val documentNumber: String?
) {
    companion object {
        const val INDEX_NAME = "registration"
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun fromEntity(entity: Registration): RegistrationDocument {
            val geoJson = GeoJsonUtils.toGeoJson(entity.geometry)
            val center = buildCenter(entity.geometry?.centroid)

            return RegistrationDocument(
                id = entity.id,
                originRegistrationId = entity.originRegistrationId,
                realEstateNumber = entity.realEstateNumber,
                registrationType = entity.registrationType,
                address = entity.address,
                roadAddress = entity.roadAddress,
                addressDetail = entity.addressDetail,
                pnuId = entity.pnuId,
                regionCode = entity.regionCode,
                sidoCode = entity.sidoCode,
                sggCode = entity.sggCode,
                umdCode = entity.umdCode,
                riCode = entity.riCode,
                registrationProcess = entity.registrationProcess,
                completedAt = entity.completedAt?.format(dateTimeFormatter),
                property = entity.property,
                createdAt = entity.createdAt.format(dateTimeFormatter),
                createdBy = entity.createdBy,
                updatedAt = entity.updatedAt?.format(dateTimeFormatter),
                updatedBy = entity.updatedBy,
                isCollateralAndLease = entity.isCollateralAndLease,
                isSalesList = entity.isSalesList,
                isExpunged = entity.isExpunged,
                isEventIgnore = entity.isEventIgnore,
                userId = entity.userId,
                isReopen = entity.isReopen,
                geometry = geoJson,
                center = center,
                transactionNumber = entity.transactionNumber,
                isLatest = entity.isLatest,
                shareScope = entity.shareScope,
                returnCode = entity.returnCode,
                refundStatus = entity.refundStatus,
                approvalNumber = entity.approvalNumber,
                registerMasterId = entity.registerMasterId,
                permanentDocumentType = entity.permanentDocumentType,
                documentNumber = entity.documentNumber
            )
        }

        private fun buildCenter(centroid: Point?): Map<String, Double>? {
            if (centroid == null) return null
            return mapOf("lat" to centroid.y, "lon" to centroid.x)
        }
    }
}
