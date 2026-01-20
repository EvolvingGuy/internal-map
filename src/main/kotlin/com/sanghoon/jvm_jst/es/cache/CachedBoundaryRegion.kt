package com.sanghoon.jvm_jst.es.cache

import org.locationtech.jts.geom.Geometry

/**
 * geo.csv.zst에서 로드된 경계 영역 데이터
 * JPA Entity와 별개로 메모리 캐시용 데이터 클래스
 */
data class CachedBoundaryRegion(
    val regionCode: String,
    val regionKoreanName: String,
    val geom: Geometry,
    val centerLng: Double,
    val centerLat: Double,
    val gubun: String
)
