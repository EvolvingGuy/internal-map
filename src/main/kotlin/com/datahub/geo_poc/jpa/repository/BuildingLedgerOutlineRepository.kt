package com.datahub.geo_poc.jpa.repository

import com.datahub.geo_poc.jpa.entity.BuildingLedgerOutline
import org.springframework.data.jpa.repository.JpaRepository

interface BuildingLedgerOutlineRepository : JpaRepository<BuildingLedgerOutline, String> {
    fun findByPnuIn(pnuList: Collection<String>): List<BuildingLedgerOutline>
}
