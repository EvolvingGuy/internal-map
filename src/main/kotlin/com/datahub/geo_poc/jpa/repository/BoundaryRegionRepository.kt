package com.datahub.geo_poc.jpa.repository

import com.datahub.geo_poc.jpa.entity.BoundaryRegion
import org.springframework.data.jpa.repository.JpaRepository

interface BoundaryRegionRepository : JpaRepository<BoundaryRegion, String>