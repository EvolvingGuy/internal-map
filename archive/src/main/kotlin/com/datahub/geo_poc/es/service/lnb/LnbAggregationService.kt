package com.datahub.geo_poc.es.service.lnb

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.GeoShapeRelation
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket
import org.opensearch.client.json.JsonData
import com.datahub.geo_poc.es.document.land.LnbDocument
import com.datahub.geo_poc.es.service.lsrc.LsrcQueryService
import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.LcAggFilter
import com.datahub.geo_poc.model.LcAggRegion
import com.datahub.geo_poc.model.LcAggResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * LNB (Land Nested Building) Aggregation 서비스
 * nested 타입 buildings 배열에 대한 필터링 지원
 * 필터 없으면 LSRC(고정형 클러스터) 사용
 */
@Service
class LnbAggregationService(
    @Value("\${opensearch.profile.enabled:false}") private val profileEnabled: Boolean,
    private val esClient: OpenSearchClient,
    private val lsrcQueryService: LsrcQueryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LnbDocument.INDEX_NAME
        const val SD_SIZE = 20
        const val SGG_SIZE = 300
        const val EMD_SIZE = 5000
    }

    fun aggregateBySd(bbox: BBoxRequest, filter: LcAggFilter = LcAggFilter()): LcAggResponse {
        if (!filter.hasAnyFilter()) {
            return lsrcQueryService.findByBbox("SD", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat).toLcAggResponse("SD")
        }
        return aggregate("sd", SD_SIZE, bbox, filter)
    }

    fun aggregateBySgg(bbox: BBoxRequest, filter: LcAggFilter = LcAggFilter()): LcAggResponse {
        if (!filter.hasAnyFilter()) {
            return lsrcQueryService.findByBbox("SGG", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat).toLcAggResponse("SGG")
        }
        return aggregate("sgg", SGG_SIZE, bbox, filter)
    }

    fun aggregateByEmd(bbox: BBoxRequest, filter: LcAggFilter = LcAggFilter()): LcAggResponse {
        if (!filter.hasAnyFilter()) {
            return lsrcQueryService.findByBbox("EMD", bbox.swLng, bbox.swLat, bbox.neLng, bbox.neLat).toLcAggResponse("EMD")
        }
        return aggregate("emd", EMD_SIZE, bbox, filter)
    }

    private fun aggregate(field: String, size: Int, bbox: BBoxRequest, filter: LcAggFilter): LcAggResponse {
        val startTime = System.currentTimeMillis()

        val envelopeJson = mapOf(
            "type" to "envelope",
            "coordinates" to listOf(listOf(bbox.swLng, bbox.neLat), listOf(bbox.neLng, bbox.swLat))
        )

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(0)
                .profile(profileEnabled)
                .query { q ->
                    q.bool { bool ->
                        // geo_shape intersects
                        bool.must { must ->
                            must.geoShape { geo ->
                                geo.field("land.geometry")
                                    .shape { shape ->
                                        shape.shape(JsonData.of(envelopeJson))
                                            .relation(GeoShapeRelation.Intersects)
                                    }
                            }
                        }
                        // 필터 적용 (nested 포함)
                        applyFilters(bool, filter)
                        bool
                    }
                }
                .aggregations("by_region") { agg ->
                    agg.terms { t -> t.field(field).size(size) }
                        .aggregations("center") { subAgg ->
                            subAgg.geoCentroid { gc -> gc.field("land.center") }
                        }
                }
        }, Void::class.java)

        val buckets = response.aggregations()["by_region"]?.sterms()?.buckets()?.array() ?: emptyList()

        val regions = buckets.map { bucket -> toRegion(bucket) }
        val totalCount = regions.sumOf { it.count }
        val elapsed = System.currentTimeMillis() - startTime

        logProfile(response, field)
        log.info("[LNB Agg] field={}, regions={}, totalCount={}, hasFilter={}, elapsed={}ms",
            field, regions.size, totalCount, filter.hasAnyFilter(), elapsed)

        return LcAggResponse(
            level = field.uppercase(),
            totalCount = totalCount,
            regionCount = regions.size,
            regions = regions,
            elapsedMs = elapsed
        )
    }

    private fun applyFilters(bool: BoolQuery.Builder, filter: LcAggFilter) {
        // Building filters - nested query로 감싸서 적용
        val buildingFilters = buildBuildingNestedFilters(filter)
        if (buildingFilters.isNotEmpty()) {
            bool.filter { f ->
                f.nested { n ->
                    n.path("buildings")
                        .query { q ->
                            q.bool { nestedBool ->
                                buildingFilters.forEach { nestedBool.filter(it) }
                                nestedBool
                            }
                        }
                }
            }
        }

        // Land filters (기존과 동일)
        if (!filter.landJiyukCd1.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("land.jiyukCd1").terms { tv -> tv.value(filter.landJiyukCd1.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } }
        }
        if (!filter.landJimokCd.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("land.jimokCd").terms { tv -> tv.value(filter.landJimokCd.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } }
        }
        if (filter.landAreaMin != null) {
            bool.filter { f -> f.range { r -> r.field("land.area").gte(JsonData.of(filter.landAreaMin)) } }
        }
        if (filter.landAreaMax != null) {
            bool.filter { f -> f.range { r -> r.field("land.area").lte(JsonData.of(filter.landAreaMax)) } }
        }
        if (filter.landPriceMin != null) {
            bool.filter { f -> f.range { r -> r.field("land.price").gte(JsonData.of(filter.landPriceMin.toDouble())) } }
        }
        if (filter.landPriceMax != null) {
            bool.filter { f -> f.range { r -> r.field("land.price").lte(JsonData.of(filter.landPriceMax.toDouble())) } }
        }

        // Trade filters (기존과 동일)
        if (!filter.tradeProperty.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("lastRealEstateTrade.property").terms { tv -> tv.value(filter.tradeProperty.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } }
        }
        if (filter.tradeContractDateStart != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.contractDate").gte(JsonData.of(filter.tradeContractDateStart.toString())) } }
        }
        if (filter.tradeContractDateEnd != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.contractDate").lte(JsonData.of(filter.tradeContractDateEnd.toString())) } }
        }
        if (filter.tradeEffectiveAmountMin != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.effectiveAmount").gte(JsonData.of(filter.tradeEffectiveAmountMin.toDouble())) } }
        }
        if (filter.tradeEffectiveAmountMax != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.effectiveAmount").lte(JsonData.of(filter.tradeEffectiveAmountMax.toDouble())) } }
        }
        if (filter.tradeBuildingAmountPerM2Min != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.buildingAmountPerM2").gte(JsonData.of(filter.tradeBuildingAmountPerM2Min.toDouble())) } }
        }
        if (filter.tradeBuildingAmountPerM2Max != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.buildingAmountPerM2").lte(JsonData.of(filter.tradeBuildingAmountPerM2Max.toDouble())) } }
        }
        if (filter.tradeLandAmountPerM2Min != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.landAmountPerM2").gte(JsonData.of(filter.tradeLandAmountPerM2Min.toDouble())) } }
        }
        if (filter.tradeLandAmountPerM2Max != null) {
            bool.filter { f -> f.range { r -> r.field("lastRealEstateTrade.landAmountPerM2").lte(JsonData.of(filter.tradeLandAmountPerM2Max.toDouble())) } }
        }
    }

    /**
     * Building 필터들을 nested query용 Query 리스트로 변환
     * 각 필터 조건은 AND로 결합됨 (하나의 건물이 모든 조건을 만족해야 함)
     */
    private fun buildBuildingNestedFilters(filter: LcAggFilter): List<Query> {
        val queries = mutableListOf<Query>()

        if (!filter.buildingMainPurpsCdNm.isNullOrEmpty()) {
            queries.add(Query.of { q ->
                q.terms { t ->
                    t.field("buildings.mainPurpsCdNm")
                        .terms { tv -> tv.value(filter.buildingMainPurpsCdNm.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) }
                }
            })
        }

        if (!filter.buildingRegstrGbCdNm.isNullOrEmpty()) {
            queries.add(Query.of { q ->
                q.terms { t ->
                    t.field("buildings.regstrGbCdNm")
                        .terms { tv -> tv.value(filter.buildingRegstrGbCdNm.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) }
                }
            })
        }

        if (filter.buildingPmsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            queries.add(Query.of { q ->
                q.range { r -> r.field("buildings.pmsDay").gte(JsonData.of(fiveYearsAgo.toString())) }
            })
        }

        if (filter.buildingStcnsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            queries.add(Query.of { q ->
                q.range { r -> r.field("buildings.stcnsDay").gte(JsonData.of(fiveYearsAgo.toString())) }
            })
        }

        if (filter.buildingUseAprDayStart != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.field("buildings.useAprDay").gte(JsonData.of("${filter.buildingUseAprDayStart}-01-01")) }
            })
        }

        if (filter.buildingUseAprDayEnd != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.field("buildings.useAprDay").lte(JsonData.of("${filter.buildingUseAprDayEnd}-12-31")) }
            })
        }

        if (filter.buildingTotAreaMin != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.field("buildings.totArea").gte(JsonData.of(filter.buildingTotAreaMin.toDouble())) }
            })
        }

        if (filter.buildingTotAreaMax != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.field("buildings.totArea").lte(JsonData.of(filter.buildingTotAreaMax.toDouble())) }
            })
        }

        if (filter.buildingPlatAreaMin != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.field("buildings.platArea").gte(JsonData.of(filter.buildingPlatAreaMin.toDouble())) }
            })
        }

        if (filter.buildingPlatAreaMax != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.field("buildings.platArea").lte(JsonData.of(filter.buildingPlatAreaMax.toDouble())) }
            })
        }

        if (filter.buildingArchAreaMin != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.field("buildings.archArea").gte(JsonData.of(filter.buildingArchAreaMin.toDouble())) }
            })
        }

        if (filter.buildingArchAreaMax != null) {
            queries.add(Query.of { q ->
                q.range { r -> r.field("buildings.archArea").lte(JsonData.of(filter.buildingArchAreaMax.toDouble())) }
            })
        }

        return queries
    }

    private fun toRegion(bucket: StringTermsBucket): LcAggRegion {
        val code = bucket.key()
        val count = bucket.docCount()
        val centroid = bucket.aggregations()["center"]?.geoCentroid()?.location()

        return LcAggRegion(
            code = code,
            name = null,
            count = count,
            centerLat = centroid?.latlon()?.lat() ?: 0.0,
            centerLng = centroid?.latlon()?.lon() ?: 0.0
        )
    }

    private fun logProfile(response: org.opensearch.client.opensearch.core.SearchResponse<Void>, field: String) {
        val profile = response.profile() ?: return
        val shards = profile.shards()

        shards.forEachIndexed { idx, shardProfile ->
            shardProfile.searches().forEach { search ->
                search.query().forEach { queryProfile ->
                    val timeMs = String.format("%.2f", queryProfile.timeInNanos() / 1_000_000.0)
                    log.info("[LNB Agg Profile] field={}, 샤드[{}] {} - {}ms", field, idx, queryProfile.type(), timeMs)
                }
            }
        }
    }
}
