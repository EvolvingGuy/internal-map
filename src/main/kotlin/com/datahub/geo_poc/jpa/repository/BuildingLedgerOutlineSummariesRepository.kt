package com.datahub.geo_poc.jpa.repository

import com.datahub.geo_poc.jpa.entity.BuildingLedgerOutlineSummaries
import org.springframework.data.jpa.repository.JpaRepository

interface BuildingLedgerOutlineSummariesRepository : JpaRepository<BuildingLedgerOutlineSummaries, String> {
    fun findByPnuIn(pnuList: Collection<String>): List<BuildingLedgerOutlineSummaries>
}
