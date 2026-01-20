package com.sanghoon.jvm_jst.es.document

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.GeoPointField
import org.springframework.data.elasticsearch.core.geo.GeoPoint
import java.time.LocalDate

/**
 * 토지 어그리게이션용 ES 인덱스 문서
 * - 지역코드(bjdong_cd) 기반 1차 탐색
 * - JVM에서 좌표 공간 탐색 2차 처리
 */
@Document(indexName = "land_aggregation")
data class LandAggregationDocument(
    @Id
    val pnu: String,

    @Field(type = FieldType.Keyword)
    val bjdongCd: String? = null,

    @Field(type = FieldType.Keyword)
    val bjdongNm: String? = null,

    @Field(type = FieldType.Keyword)
    val sidoCd: String? = null,

    @Field(type = FieldType.Keyword)
    val sggCd: String? = null,

    @Field(type = FieldType.Keyword)
    val emdCd: String? = null,

    @Field(type = FieldType.Keyword)
    val regstrGbCd: String? = null,

    @Field(type = FieldType.Keyword)
    val jibun: String? = null,

    @Field(type = FieldType.Keyword)
    val jimokCd: String? = null,

    @Field(type = FieldType.Keyword)
    val jimok: String? = null,

    @Field(type = FieldType.Double)
    val area: Double? = null,

    @Field(type = FieldType.Keyword)
    val jiyukCd1: String? = null,

    @Field(type = FieldType.Keyword)
    val jiyuk1: String? = null,

    @Field(type = FieldType.Keyword)
    val landUseCd: String? = null,

    @Field(type = FieldType.Keyword)
    val landUse: String? = null,

    @Field(type = FieldType.Keyword)
    val shapeCd: String? = null,

    @Field(type = FieldType.Keyword)
    val roadCd: String? = null,

    @Field(type = FieldType.Double)
    val price: Double? = null,

    @Field(type = FieldType.Date)
    val crtnDay: LocalDate? = null,

    @GeoPointField
    val center: GeoPoint? = null,

    @Field(type = FieldType.Double)
    val centerLat: Double? = null,

    @Field(type = FieldType.Double)
    val centerLng: Double? = null,

    @Field(type = FieldType.Boolean)
    val isDonut: Boolean? = null

    // TODO: 추후 building_ledger, real_estate_trade 연동 시 컬럼 추가
)
