package com.datahub.geo_poc.es.service.lc

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.GeoShapeRelation
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket
import org.opensearch.client.json.JsonData
import com.datahub.geo_poc.es.document.land.LandCompactDocument
import com.datahub.geo_poc.model.BBoxRequest
import com.datahub.geo_poc.model.GridParamsRequest
import com.datahub.geo_poc.model.LcAggFilter
import com.datahub.geo_poc.model.LcGridCell
import com.datahub.geo_poc.model.LcGridAggResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.ceil

/**
 * LC Grid Aggregation 서비스
 * 뷰포트를 바둑판(그리드)으로 나눠서 land_compact 집계
 */
@Service
class LcGridAggregationService(
    @Value("\${opensearch.profile.enabled:false}") private val profileEnabled: Boolean,
    private val esClient: OpenSearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LandCompactDocument.INDEX_NAME
        const val MAX_GRID_CELLS = 500  // 최대 그리드 셀 수
    }

    fun aggregate(bbox: BBoxRequest, gridParams: GridParamsRequest, filter: LcAggFilter = LcAggFilter()): LcGridAggResponse {
        val startTime = System.currentTimeMillis()

        // 그리드 계산
        val cols = ceil(gridParams.viewportWidth.toDouble() / gridParams.gridSize).toInt()
        val rows = ceil(gridParams.viewportHeight.toDouble() / gridParams.gridSize).toInt()
        val cellLng = (bbox.neLng - bbox.swLng) / cols
        val cellLat = (bbox.neLat - bbox.swLat) / rows

        log.info("[LC Grid] cols={}, rows={}, cellLng={}, cellLat={}", cols, rows, cellLng, cellLat)

        // envelope GeoJSON: [[left, top], [right, bottom]]
        val envelopeJson = mapOf(
            "type" to "envelope",
            "coordinates" to listOf(listOf(bbox.swLng, bbox.neLat), listOf(bbox.neLng, bbox.swLat))
        )

        // ES scripted terms aggregation
        val script = """
            int x = (int)((doc['land.center'].lon - params.minLng) / params.cellLng);
            int y = (int)((doc['land.center'].lat - params.minLat) / params.cellLat);
            if (x < 0) x = 0;
            if (y < 0) y = 0;
            if (x >= params.cols) x = params.cols - 1;
            if (y >= params.rows) y = params.rows - 1;
            return x + '_' + y;
        """.trimIndent()

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(0)
                .profile(profileEnabled)
                .query { q ->
                    q.bool { bool ->
                        // geo_shape intersects 쿼리
                        bool.must { must ->
                            must.geoShape { geo ->
                                geo.field("land.geometry")
                                    .shape { shape ->
                                        shape.shape(JsonData.of(envelopeJson))
                                            .relation(GeoShapeRelation.Intersects)
                                    }
                            }
                        }
                        // 필터 적용
                        applyFilters(bool, filter)
                        bool
                    }
                }
                .aggregations("grid") { agg ->
                    val scriptParams = mapOf(
                        "minLng" to org.opensearch.client.json.JsonData.of(bbox.swLng),
                        "minLat" to org.opensearch.client.json.JsonData.of(bbox.swLat),
                        "cellLng" to org.opensearch.client.json.JsonData.of(cellLng),
                        "cellLat" to org.opensearch.client.json.JsonData.of(cellLat),
                        "cols" to org.opensearch.client.json.JsonData.of(cols),
                        "rows" to org.opensearch.client.json.JsonData.of(rows)
                    )
                    agg.terms { t ->
                        t.script { s ->
                            s.inline { i -> i.source(script).params(scriptParams) }
                        }.size(MAX_GRID_CELLS)
                    }.aggregations("center") { subAgg ->
                        subAgg.geoCentroid { gc -> gc.field("land.center") }
                    }
                }
        }, Void::class.java)
        logProfile(response)

        val buckets = response.aggregations()["grid"]?.sterms()?.buckets()?.array() ?: emptyList()

        val cells = buckets.map { bucket -> toGridCell(bucket) }
        val totalCount = cells.sumOf { it.count }
        val elapsed = System.currentTimeMillis() - startTime

        log.info("[LC Grid] cols={}, rows={}, cells={}, totalCount={}, hasFilter={}, elapsed={}ms",
            cols, rows, cells.size, totalCount, filter.hasAnyFilter(), elapsed)

        return LcGridAggResponse(
            cols = cols,
            rows = rows,
            totalCount = totalCount,
            cellCount = cells.size,
            cells = cells,
            elapsedMs = elapsed
        )
    }

    private fun toGridCell(bucket: StringTermsBucket): LcGridCell {
        val key = bucket.key()
        val parts = key.split("_")
        val gridX = parts[0].toInt()
        val gridY = parts[1].toInt()
        val count = bucket.docCount()
        val centroid = bucket.aggregations()["center"]?.geoCentroid()?.location()

        return LcGridCell(
            gridX = gridX,
            gridY = gridY,
            count = count,
            centerLat = centroid?.latlon()?.lat() ?: 0.0,
            centerLng = centroid?.latlon()?.lon() ?: 0.0
        )
    }

    private fun applyFilters(bool: BoolQuery.Builder, filter: LcAggFilter) {
        // Building filters
        if (!filter.buildingMainPurpsCdNm.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("building.mainPurpsCdNm").terms { tv -> tv.value(filter.buildingMainPurpsCdNm.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } }
        }
        if (!filter.buildingRegstrGbCdNm.isNullOrEmpty()) {
            bool.filter { f -> f.terms { t -> t.field("building.regstrGbCdNm").terms { tv -> tv.value(filter.buildingRegstrGbCdNm.map { org.opensearch.client.opensearch._types.FieldValue.of(it) }) } } }
        }
        if (filter.buildingPmsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            bool.filter { f -> f.range { r -> r.field("building.pmsDay").gte(JsonData.of(fiveYearsAgo.toString())) } }
        }
        if (filter.buildingStcnsDayRecent5y == true) {
            val fiveYearsAgo = LocalDate.now().minusYears(5)
            bool.filter { f -> f.range { r -> r.field("building.stcnsDay").gte(JsonData.of(fiveYearsAgo.toString())) } }
        }
        if (filter.buildingUseAprDayStart != null) {
            bool.filter { f -> f.range { r -> r.field("building.useAprDay").gte(JsonData.of("${filter.buildingUseAprDayStart}-01-01")) } }
        }
        if (filter.buildingUseAprDayEnd != null) {
            bool.filter { f -> f.range { r -> r.field("building.useAprDay").lte(JsonData.of("${filter.buildingUseAprDayEnd}-12-31")) } }
        }
        if (filter.buildingTotAreaMin != null) {
            bool.filter { f -> f.range { r -> r.field("building.totArea").gte(JsonData.of(filter.buildingTotAreaMin.toDouble())) } }
        }
        if (filter.buildingTotAreaMax != null) {
            bool.filter { f -> f.range { r -> r.field("building.totArea").lte(JsonData.of(filter.buildingTotAreaMax.toDouble())) } }
        }
        if (filter.buildingPlatAreaMin != null) {
            bool.filter { f -> f.range { r -> r.field("building.platArea").gte(JsonData.of(filter.buildingPlatAreaMin.toDouble())) } }
        }
        if (filter.buildingPlatAreaMax != null) {
            bool.filter { f -> f.range { r -> r.field("building.platArea").lte(JsonData.of(filter.buildingPlatAreaMax.toDouble())) } }
        }
        if (filter.buildingArchAreaMin != null) {
            bool.filter { f -> f.range { r -> r.field("building.archArea").gte(JsonData.of(filter.buildingArchAreaMin.toDouble())) } }
        }
        if (filter.buildingArchAreaMax != null) {
            bool.filter { f -> f.range { r -> r.field("building.archArea").lte(JsonData.of(filter.buildingArchAreaMax.toDouble())) } }
        }

        // Land filters
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

        // Trade filters
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

    private fun logProfile(response: org.opensearch.client.opensearch.core.SearchResponse<Void>) {
        val profile = response.profile() ?: return
        val shards = profile.shards()

        shards.forEachIndexed { idx, shardProfile ->
            shardProfile.searches().forEach { search ->
                search.query().forEach { queryProfile ->
                    val timeMs = String.format("%.2f", queryProfile.timeInNanos() / 1_000_000.0)
                    log.info("[LC Grid Profile] 샤드[{}] {} - {}ms", idx, queryProfile.type(), timeMs)
                }
            }
        }
    }
}
