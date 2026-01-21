package com.sanghoon.jvm_jst.es.document

import com.sanghoon.jvm_jst.rds.repository.RegistrationIndexRow
import java.time.LocalDateTime
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
    val completedAt: String?,        // ISO date format
    val property: String,
    val createdAt: String,           // ISO date format
    val createdBy: Long,
    val updatedAt: String?,          // ISO date format
    val updatedBy: Long?,
    val isCollateralAndLease: Boolean?,
    val isSalesList: Boolean?,
    val isExpunged: Boolean?,
    val isEventIgnore: Boolean?,
    val userId: Long?,
    val isReopen: Boolean,
    val geometry: Map<String, Any>?, // GeoJSON for geo_shape
    val center: Map<String, Double>?, // { "lat": ..., "lon": ... } for geo_point
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

        fun fromRow(row: RegistrationIndexRow): RegistrationDocument {
            return RegistrationDocument(
                id = row.id,
                originRegistrationId = row.originRegistrationId,
                realEstateNumber = row.realEstateNumber,
                registrationType = row.registrationType,
                address = row.address,
                roadAddress = row.roadAddress,
                addressDetail = row.addressDetail,
                pnuId = row.pnuId,
                regionCode = row.regionCode,
                sidoCode = row.sidoCode,
                sggCode = row.sggCode,
                umdCode = row.umdCode,
                riCode = row.riCode,
                registrationProcess = row.registrationProcess,
                completedAt = row.completedAt?.format(dateTimeFormatter),
                property = row.property,
                createdAt = row.createdAt.format(dateTimeFormatter),
                createdBy = row.createdBy,
                updatedAt = row.updatedAt?.format(dateTimeFormatter),
                updatedBy = row.updatedBy,
                isCollateralAndLease = row.isCollateralAndLease,
                isSalesList = row.isSalesList,
                isExpunged = row.isExpunged,
                isEventIgnore = row.isEventIgnore,
                userId = row.userId,
                isReopen = row.isReopen,
                geometry = parseGeoJson(row.geometryGeoJson),
                center = buildCenter(row.centerLat, row.centerLng),
                transactionNumber = row.transactionNumber,
                isLatest = row.isLatest,
                shareScope = row.shareScope,
                returnCode = row.returnCode,
                refundStatus = row.refundStatus,
                approvalNumber = row.approvalNumber,
                registerMasterId = row.registerMasterId,
                permanentDocumentType = row.permanentDocumentType,
                documentNumber = row.documentNumber
            )
        }

        private fun parseGeoJson(geoJson: String?): Map<String, Any>? {
            if (geoJson.isNullOrBlank()) return null
            return try {
                // GeoJSON을 Map으로 파싱 (Jackson 사용)
                val mapper = com.fasterxml.jackson.databind.ObjectMapper()
                @Suppress("UNCHECKED_CAST")
                mapper.readValue(geoJson, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                null
            }
        }

        private fun buildCenter(lat: Double?, lng: Double?): Map<String, Double>? {
            if (lat == null || lng == null) return null
            return mapOf("lat" to lat, "lon" to lng)
        }
    }
}
