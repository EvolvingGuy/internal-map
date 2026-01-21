package com.sanghoon.jvm_jst.rds.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Registration 인덱싱용 Row
 */
data class RegistrationIndexRow(
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
    val completedAt: LocalDateTime?,
    val property: String,
    val createdAt: LocalDateTime,
    val createdBy: Long,
    val updatedAt: LocalDateTime?,
    val updatedBy: Long?,
    val isCollateralAndLease: Boolean?,
    val isSalesList: Boolean?,
    val isExpunged: Boolean?,
    val isEventIgnore: Boolean?,
    val userId: Long?,
    val isReopen: Boolean,
    val geometryGeoJson: String?,  // GeoJSON 형태
    val centerLat: Double?,        // ST_Centroid 결과
    val centerLng: Double?,
    val transactionNumber: String?,
    val isLatest: Boolean?,
    val shareScope: String?,
    val returnCode: String?,
    val refundStatus: String?,
    val approvalNumber: String?,
    val registerMasterId: Long?,
    val permanentDocumentType: String?,
    val documentNumber: String?
)

/**
 * Registration 커서 기반 Repository (ES 인덱싱용)
 */
@Repository
class RegistrationCursorRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    /**
     * 전체 건수 조회
     */
    fun countAll(): Long {
        val sql = "SELECT COUNT(*) FROM manage.r3_registration"
        return jdbcTemplate.queryForObject(sql, Long::class.java) ?: 0
    }

    /**
     * ID 기반 파티션 경계 조회
     * @param partitions 분할 수
     * @return ID 경계 리스트 (partitions + 1 개)
     */
    fun findIdBoundaries(partitions: Int): List<Int> {
        // min, max id 조회
        val minMax = jdbcTemplate.queryForMap(
            "SELECT MIN(id) as min_id, MAX(id) as max_id FROM manage.r3_registration"
        )
        val minId = (minMax["min_id"] as? Number)?.toInt() ?: 0
        val maxId = (minMax["max_id"] as? Number)?.toInt() ?: 0

        if (minId == 0 && maxId == 0) return listOf(0, 0)

        val range = maxId - minId + 1
        val chunkSize = (range + partitions - 1) / partitions

        val boundaries = mutableListOf<Int>()
        for (i in 0 until partitions) {
            boundaries.add(minId + i * chunkSize)
        }
        boundaries.add(maxId + 1)  // exclusive upper bound

        return boundaries
    }

    /**
     * ID 범위로 커서 기반 조회
     * @param minId 시작 ID (inclusive)
     * @param maxId 종료 ID (exclusive)
     * @param lastId 마지막 조회 ID (커서), null이면 처음부터
     * @param limit 조회 건수
     */
    fun findByIdRange(
        minId: Int,
        maxId: Int,
        lastId: Int?,
        limit: Int
    ): List<RegistrationIndexRow> {
        val sql = if (lastId == null) {
            """
                SELECT
                    id, origin_registration_id, real_estate_number, registration_type,
                    address, road_address, address_detail, pnu_id, region_code,
                    sido_code, sgg_code, umd_code, ri_code,
                    registration_process, completed_at, property,
                    created_at, created_by, updated_at, updated_by,
                    is_collateral_and_lease, is_sales_list, is_expunged, is_event_ignore,
                    user_id, is_reopen,
                    ST_AsGeoJSON(geometry) as geometry_geojson,
                    ST_Y(ST_Centroid(geometry)) as center_lat,
                    ST_X(ST_Centroid(geometry)) as center_lng,
                    transaction_number, is_latest, share_scope,
                    return_code, refund_status, approval_number,
                    register_master_id, permanent_document_type, document_number
                FROM manage.r3_registration
                WHERE id >= ? AND id < ?
                ORDER BY id
                LIMIT ?
            """.trimIndent()
        } else {
            """
                SELECT
                    id, origin_registration_id, real_estate_number, registration_type,
                    address, road_address, address_detail, pnu_id, region_code,
                    sido_code, sgg_code, umd_code, ri_code,
                    registration_process, completed_at, property,
                    created_at, created_by, updated_at, updated_by,
                    is_collateral_and_lease, is_sales_list, is_expunged, is_event_ignore,
                    user_id, is_reopen,
                    ST_AsGeoJSON(geometry) as geometry_geojson,
                    ST_Y(ST_Centroid(geometry)) as center_lat,
                    ST_X(ST_Centroid(geometry)) as center_lng,
                    transaction_number, is_latest, share_scope,
                    return_code, refund_status, approval_number,
                    register_master_id, permanent_document_type, document_number
                FROM manage.r3_registration
                WHERE id >= ? AND id < ? AND id > ?
                ORDER BY id
                LIMIT ?
            """.trimIndent()
        }

        val params = if (lastId == null) {
            arrayOf(minId, maxId, limit)
        } else {
            arrayOf(minId, maxId, lastId, limit)
        }

        return jdbcTemplate.query(sql, params) { rs, _ ->
            RegistrationIndexRow(
                id = rs.getInt("id"),
                originRegistrationId = rs.getObject("origin_registration_id") as? Int,
                realEstateNumber = rs.getString("real_estate_number"),
                registrationType = rs.getString("registration_type"),
                address = rs.getString("address"),
                roadAddress = rs.getString("road_address"),
                addressDetail = rs.getString("address_detail"),
                pnuId = rs.getString("pnu_id"),
                regionCode = rs.getString("region_code"),
                sidoCode = rs.getString("sido_code"),
                sggCode = rs.getString("sgg_code"),
                umdCode = rs.getString("umd_code"),
                riCode = rs.getString("ri_code"),
                registrationProcess = rs.getString("registration_process"),
                completedAt = rs.getTimestamp("completed_at")?.toLocalDateTime(),
                property = rs.getString("property"),
                createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                createdBy = rs.getLong("created_by"),
                updatedAt = rs.getTimestamp("updated_at")?.toLocalDateTime(),
                updatedBy = rs.getObject("updated_by") as? Long,
                isCollateralAndLease = rs.getObject("is_collateral_and_lease") as? Boolean,
                isSalesList = rs.getObject("is_sales_list") as? Boolean,
                isExpunged = rs.getObject("is_expunged") as? Boolean,
                isEventIgnore = rs.getObject("is_event_ignore") as? Boolean,
                userId = rs.getObject("user_id") as? Long,
                isReopen = rs.getBoolean("is_reopen"),
                geometryGeoJson = rs.getString("geometry_geojson"),
                centerLat = rs.getObject("center_lat") as? Double,
                centerLng = rs.getObject("center_lng") as? Double,
                transactionNumber = rs.getString("transaction_number"),
                isLatest = rs.getObject("is_latest") as? Boolean,
                shareScope = rs.getString("share_scope"),
                returnCode = rs.getString("return_code"),
                refundStatus = rs.getString("refund_status"),
                approvalNumber = rs.getString("approval_number"),
                registerMasterId = rs.getObject("register_master_id") as? Long,
                permanentDocumentType = rs.getString("permanent_document_type"),
                documentNumber = rs.getString("document_number")
            )
        }
    }
}
