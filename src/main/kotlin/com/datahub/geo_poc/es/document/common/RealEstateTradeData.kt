package com.datahub.geo_poc.es.document.common

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 실거래 데이터 (공유)
 */
data class RealEstateTradeData(
    val property: String,
    val contractDate: LocalDate,
    val effectiveAmount: Long,
    val buildingAmountPerM2: BigDecimal?,
    val landAmountPerM2: BigDecimal?
)
