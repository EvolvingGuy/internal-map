package com.datahub.geo_poc.jpa.repository

import com.datahub.geo_poc.jpa.entity.PnuAggEmd10
import jakarta.persistence.QueryHint
import org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.QueryHints
import java.util.stream.Stream

interface PnuAggEmd10Repository : JpaRepository<PnuAggEmd10, Long> {

    @QueryHints(QueryHint(name = HINT_FETCH_SIZE, value = "10000"))
    fun findAllBy(): Stream<PnuAggEmd10>
}
