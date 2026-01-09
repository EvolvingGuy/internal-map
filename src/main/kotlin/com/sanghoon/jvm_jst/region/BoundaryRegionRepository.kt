package com.sanghoon.jvm_jst.region

import org.springframework.data.jpa.repository.JpaRepository

interface BoundaryRegionRepository : JpaRepository<BoundaryRegionEntity, String> {

    fun findByRegionCodeIn(regionCodes: Collection<String>): List<BoundaryRegionEntity>
}