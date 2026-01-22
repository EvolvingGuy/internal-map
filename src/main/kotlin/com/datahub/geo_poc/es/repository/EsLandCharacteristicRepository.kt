package com.datahub.geo_poc.es.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import com.datahub.geo_poc.jpa.entity.LandCharacteristic

interface EsLandCharacteristicRepository : JpaRepository<LandCharacteristic, String> {

    /**
     * 시도코드 기준 커서 조회 (첫 페이지)
     */
    @Query("SELECT l FROM LandCharacteristic l WHERE l.pnu LIKE :sidoPrefix ORDER BY l.pnu")
    fun findBySidoFirst(sidoPrefix: String, pageable: Pageable): List<LandCharacteristic>

    /**
     * 시도코드 기준 커서 조회 (이후 페이지)
     */
    @Query("SELECT l FROM LandCharacteristic l WHERE l.pnu LIKE :sidoPrefix AND l.pnu > :cursor ORDER BY l.pnu")
    fun findBySidoCursor(sidoPrefix: String, cursor: String, pageable: Pageable): List<LandCharacteristic>

    /**
     * 시도코드 기준 카운트
     */
    @Query("SELECT COUNT(l) FROM LandCharacteristic l WHERE l.pnu LIKE :sidoPrefix")
    fun countBySido(sidoPrefix: String): Long
}
