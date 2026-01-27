package com.datahub.geo_poc.es.document.land

import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData

/**
 * LCP (Land Compact Point) ES 문서
 * 필지 단위 인덱스 - geometry 없이 center(geo_point)만 저장하여 용량 최소화
 * LC 대비 용량 감소, bbox 쿼리로 조회
 */
data class LcpDocument(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    val building: BuildingData?,
    val lastRealEstateTrade: RealEstateTradeData?
) {
    companion object {
        const val INDEX_NAME = "land_compact_point"
    }

    data class Land(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?,
        val center: Map<String, Double>
        // geometry 없음 - LC와의 핵심 차이점
    )
}
