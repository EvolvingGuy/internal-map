package com.datahub.geo_poc.jpa.repository

import com.datahub.geo_poc.jpa.entity.Registration
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * Registration 커서 기반 Repository (ES 인덱싱용)
 */
interface RegistrationCursorRepository : JpaRepository<Registration, Int> {

    /**
     * MIN/MAX ID 조회
     */
    @Query("SELECT MIN(r.id), MAX(r.id) FROM Registration r")
    fun findMinMaxId(): Array<Any?>

    /**
     * ID 범위로 첫 페이지 조회 (커서 없음)
     */
    @Query("SELECT r FROM Registration r WHERE r.id >= :minId AND r.id < :maxId ORDER BY r.id")
    fun findByIdRangeFirst(
        @Param("minId") minId: Int,
        @Param("maxId") maxId: Int,
        pageable: Pageable
    ): List<Registration>

    /**
     * ID 범위로 커서 기반 조회 (lastId 이후부터)
     */
    @Query("SELECT r FROM Registration r WHERE r.id >= :minId AND r.id < :maxId AND r.id > :lastId ORDER BY r.id")
    fun findByIdRangeCursor(
        @Param("minId") minId: Int,
        @Param("maxId") maxId: Int,
        @Param("lastId") lastId: Int,
        pageable: Pageable
    ): List<Registration>
}
