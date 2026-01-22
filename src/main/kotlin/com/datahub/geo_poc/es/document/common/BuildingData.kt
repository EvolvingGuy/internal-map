package com.datahub.geo_poc.es.document.common

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 건축물 데이터 (공유)
 */
data class BuildingData(
    val mgmBldrgstPk: String,
    val mainPurpsCdNm: String?,
    val regstrGbCdNm: String?,
    val pmsDay: LocalDate?,
    val stcnsDay: LocalDate?,
    val useAprDay: LocalDate?,
    val totArea: BigDecimal?,
    val platArea: BigDecimal?,
    val archArea: BigDecimal?
)
