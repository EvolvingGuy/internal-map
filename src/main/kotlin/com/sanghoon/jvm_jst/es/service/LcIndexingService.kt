package com.sanghoon.jvm_jst.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import com.sanghoon.jvm_jst.entity.BuildingLedgerOutline
import com.sanghoon.jvm_jst.entity.BuildingLedgerOutlineSummaries
import com.sanghoon.jvm_jst.es.document.*
import com.sanghoon.jvm_jst.es.util.ParsingUtils
import com.sanghoon.jvm_jst.es.util.PnuUtils
import com.sanghoon.jvm_jst.rds.repository.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

/**
 * LC (Land Compact) 인덱싱 서비스
 * 필지 단위 인덱스 - 비즈니스 필터 있는 경우 사용
 */
@Service
class LcIndexingService(
    private val landCharRepo: LandCharacteristicCursorRepository,
    private val buildingSummariesRepo: BuildingLedgerOutlineSummariesRepository,
    private val buildingOutlineRepo: BuildingLedgerOutlineRepository,
    private val realEstateTradeRepo: RealEstateTradeRepository,
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LandCompactDocument.INDEX_NAME
        const val PARALLELISM = 20
        const val BATCH_SIZE = 3000
        const val BULK_SIZE = 3000
    }

    // ==================== Public API ====================

    /**
     * 전체 재인덱싱
     */
    fun reindex(): Map<String, Any> = runBlocking {
        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, Any>()

        ensureIndexExists()
        log.info("[LC] ========== 인덱싱 시작 ==========")

        val totalCount = landCharRepo.countAll()
        log.info("[LC] 전체 필지 수: {}", totalCount)

        val boundaries = landCharRepo.findPnuBoundaries(PARALLELISM)
        log.info("[LC] {} 개 워커로 병렬 처리 시작", boundaries.size - 1)

        val processedCount = AtomicInteger(0)
        val indexedCount = AtomicInteger(0)

        coroutineScope {
            val jobs = (0 until boundaries.size - 1).map { workerIndex ->
                async(Dispatchers.IO) {
                    processPartition(
                        workerIndex = workerIndex,
                        minPnu = boundaries[workerIndex],
                        maxPnu = boundaries[workerIndex + 1],
                        totalCount = totalCount,
                        processedCount = processedCount,
                        indexedCount = indexedCount
                    )
                }
            }
            jobs.awaitAll()
        }

        esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LC] ========== 인덱싱 완료 ==========")
        log.info("[LC] 처리: {}/{}, 인덱싱: {}, 소요: {}ms", processedCount.get(), totalCount, indexedCount.get(), elapsed)

        results["totalCount"] = totalCount
        results["processed"] = processedCount.get()
        results["indexed"] = indexedCount.get()
        results["elapsedMs"] = elapsed
        results["success"] = true

        results
    }

    /**
     * 인덱스 카운트
     */
    fun count(): Long {
        return try {
            esClient.count { c -> c.index(INDEX_NAME) }.count()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * forcemerge 실행
     */
    fun forcemerge(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        log.info("[LC] forcemerge 시작...")
        esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LC] forcemerge 완료: {}ms", elapsed)

        return mapOf(
            "action" to "forcemerge",
            "elapsedMs" to elapsed,
            "success" to true
        )
    }

    /**
     * 인덱스 삭제
     */
    fun deleteIndex(): Map<String, Any> {
        val exists = try {
            esClient.indices().exists { e -> e.index(INDEX_NAME) }.value()
        } catch (e: Exception) {
            false
        }

        return if (exists) {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            log.info("[LC] 인덱스 삭제 완료: {}", INDEX_NAME)
            mapOf("deleted" to true, "index" to INDEX_NAME)
        } else {
            log.info("[LC] 삭제할 인덱스 없음: {}", INDEX_NAME)
            mapOf("deleted" to false, "index" to INDEX_NAME, "reason" to "not exists")
        }
    }

    // ==================== 파티션 처리 ====================

    private suspend fun processPartition(
        workerIndex: Int,
        minPnu: String,
        maxPnu: String,
        totalCount: Long,
        processedCount: AtomicInteger,
        indexedCount: AtomicInteger
    ) {
        var lastPnu: String? = null
        var workerProcessed = 0
        var workerIndexed = 0

        log.info("[LC] Worker-{} 시작: pnu {} ~ {}", workerIndex, minPnu, maxPnu)

        while (true) {
            val t0 = System.currentTimeMillis()
            val rows = landCharRepo.findForLcIndexing(minPnu, maxPnu, lastPnu, BATCH_SIZE)
            val t1 = System.currentTimeMillis()
            if (rows.isEmpty()) break

            log.info("[LC] W-{} land_char 조회: {}ms ({}건)", workerIndex, t1 - t0, rows.size)

            val docs = processBatch(rows, workerIndex)

            if (docs.isNotEmpty()) {
                val tBulk0 = System.currentTimeMillis()
                bulkIndex(docs)
                val tBulk1 = System.currentTimeMillis()
                log.info("[LC] W-{} bulkIndex: {}ms ({}건)", workerIndex, tBulk1 - tBulk0, docs.size)
            }

            lastPnu = rows.last().pnu
            workerProcessed += rows.size
            workerIndexed += docs.size

            val globalProcessed = processedCount.addAndGet(rows.size)
            indexedCount.addAndGet(docs.size)

            if (globalProcessed % 100000 < rows.size) {
                val pct = String.format("%.1f", globalProcessed * 100.0 / totalCount)
                log.info("[LC] 진행: {}/{} ({}%), 인덱싱: {}", globalProcessed, totalCount, pct, indexedCount.get())
            }
        }

        log.info("[LC] Worker-{} 완료: {} 건 (인덱싱: {})", workerIndex, workerProcessed, workerIndexed)
    }

    private fun processBatch(
        rows: List<LandCharacteristicLcRow>,
        workerIndex: Int
    ): List<LandCompactDocument> {
        val pnuList = rows.map { it.pnu }
        val buildingPnuList = pnuList.map { PnuUtils.convertLandPnuToBuilding(it) }

        var t0 = System.currentTimeMillis()
        val summariesMap = loadBuildingSummaries(buildingPnuList, workerIndex)
        val t1 = System.currentTimeMillis()

        val outlineMap = loadBuildingOutlines(buildingPnuList, workerIndex)
        val t2 = System.currentTimeMillis()

        val tradeMap = loadRealEstateTrades(pnuList, workerIndex)
        val t3 = System.currentTimeMillis()

        log.info("[LC] W-{} 조회시간: summaries={}ms, outline={}ms, trade={}ms",
            workerIndex, t1 - t0, t2 - t1, t3 - t2)

        return rows.mapNotNull { row ->
            createDocument(row, summariesMap, outlineMap, tradeMap)
        }
    }

    // ==================== 데이터 로딩 ====================

    private fun loadBuildingSummaries(
        buildingPnuList: List<String>,
        workerIndex: Int
    ): Map<String, BuildingLedgerOutlineSummaries> {
        return try {
            buildingSummariesRepo.findByPnuIn(buildingPnuList)
                .groupBy { PnuUtils.buildPnuFrom(it) }
                .mapValues { it.value.first() }
        } catch (e: Exception) {
            log.warn("[LC] Worker-{} summaries 조회 실패: {}", workerIndex, e.message)
            emptyMap()
        }
    }

    private fun loadBuildingOutlines(
        buildingPnuList: List<String>,
        workerIndex: Int
    ): Map<String, BuildingLedgerOutline> {
        return try {
            buildingOutlineRepo.findByPnuIn(buildingPnuList)
                .groupBy { PnuUtils.buildPnuFrom(it) }
                .mapValues { it.value.first() }
        } catch (e: Exception) {
            log.warn("[LC] Worker-{} outline 조회 실패: {}", workerIndex, e.message)
            emptyMap()
        }
    }

    private fun loadRealEstateTrades(
        pnuList: List<String>,
        workerIndex: Int
    ): Map<String, RealEstateTradeProjection> {
        return try {
            realEstateTradeRepo.findLatestByPnuIn(pnuList)
                .associateBy { it.pnu }
        } catch (e: Exception) {
            log.warn("[LC] Worker-{} trade 조회 실패: {}", workerIndex, e.message)
            emptyMap()
        }
    }

    // ==================== 문서 생성 ====================

    private fun createDocument(
        row: LandCharacteristicLcRow,
        summariesMap: Map<String, BuildingLedgerOutlineSummaries>,
        outlineMap: Map<String, BuildingLedgerOutline>,
        tradeMap: Map<String, RealEstateTradeProjection>
    ): LandCompactDocument? {
        return try {
            val buildingPnu = PnuUtils.convertLandPnuToBuilding(row.pnu)

            val buildingData = summariesMap[buildingPnu]?.let { toBuildingData(it) }
                ?: outlineMap[buildingPnu]?.let { toBuildingData(it) }

            val tradeData = tradeMap[row.pnu]?.let { toRealEstateTradeData(it) }

            LandCompactDocument(
                pnu = row.pnu,
                sd = PnuUtils.extractSd(row.pnu),
                sgg = PnuUtils.extractSgg(row.pnu),
                emd = PnuUtils.extractEmd(row.pnu),
                land = toLandData(row),
                building = buildingData,
                lastRealEstateTrade = tradeData
            )
        } catch (e: Exception) {
            log.warn("[LC] 문서 생성 실패 pnu={}: {}", row.pnu, e.message)
            null
        }
    }

    // ==================== 변환 ====================

    private fun toLandData(row: LandCharacteristicLcRow): LandData {
        return LandData(
            jiyukCd1 = row.jiyukCd1?.takeIf { it.isNotBlank() },
            jimokCd = row.jimokCd?.takeIf { it.isNotBlank() },
            area = ParsingUtils.toDoubleOrNull(row.area),
            price = ParsingUtils.toLongOrNull(row.price),
            center = mapOf("lat" to row.centerLat, "lon" to row.centerLng)
        )
    }

    private fun toBuildingData(entity: BuildingLedgerOutlineSummaries): BuildingData? {
        val mgmBldrgstPk = entity.mgmBldrgstPk.takeIf { it.isNotBlank() } ?: return null
        return BuildingData(
            mgmBldrgstPk = mgmBldrgstPk,
            mainPurpsCdNm = entity.mainPurpsCdNm?.takeIf { it.isNotBlank() },
            regstrGbCdNm = entity.regstrGbCdNm?.takeIf { it.isNotBlank() },
            pmsDay = ParsingUtils.toLocalDateOrNull(entity.pmsDay),
            stcnsDay = ParsingUtils.toLocalDateOrNull(entity.stcnsDay),
            useAprDay = ParsingUtils.toLocalDateOrNull(entity.useAprDay),
            totArea = ParsingUtils.toBigDecimalOrNull(entity.totArea),
            platArea = ParsingUtils.toBigDecimalOrNull(entity.platArea),
            archArea = ParsingUtils.toBigDecimalOrNull(entity.archArea)
        )
    }

    private fun toBuildingData(entity: BuildingLedgerOutline): BuildingData? {
        val mgmBldrgstPk = entity.mgmBldrgstPk.takeIf { it.isNotBlank() } ?: return null
        return BuildingData(
            mgmBldrgstPk = mgmBldrgstPk,
            mainPurpsCdNm = entity.mainPurpsCdNm?.takeIf { it.isNotBlank() },
            regstrGbCdNm = entity.regstrGbCdNm?.takeIf { it.isNotBlank() },
            pmsDay = ParsingUtils.toLocalDateOrNull(entity.pmsDay),
            stcnsDay = ParsingUtils.toLocalDateOrNull(entity.stcnsDay),
            useAprDay = ParsingUtils.toLocalDateOrNull(entity.useAprDay),
            totArea = ParsingUtils.toBigDecimalOrNull(entity.totArea),
            platArea = ParsingUtils.toBigDecimalOrNull(entity.platArea),
            archArea = ParsingUtils.toBigDecimalOrNull(entity.archArea)
        )
    }

    private fun toRealEstateTradeData(projection: RealEstateTradeProjection): RealEstateTradeData {
        return RealEstateTradeData(
            property = projection.property,
            contractDate = projection.contractDate,
            effectiveAmount = projection.effectiveAmount,
            buildingAmountPerM2 = projection.buildingAmountPerM2,
            landAmountPerM2 = projection.landAmountPerM2
        )
    }

    // ==================== ES 인덱스 ====================

    private fun ensureIndexExists() {
        val exists = try {
            esClient.indices().exists { e -> e.index(INDEX_NAME) }.value()
        } catch (e: Exception) {
            false
        }

        if (exists) {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            log.info("[LC] 기존 인덱스 삭제")
        }

        esClient.indices().create { c ->
            c.index(INDEX_NAME)
                .settings { s ->
                    s.numberOfShards("1")
                        .numberOfReplicas("0")
                }
                .mappings { m ->
                    m.properties("pnu") { p -> p.keyword { it } }
                        .properties("sd") { p -> p.keyword { k -> k.eagerGlobalOrdinals(true) } }
                        .properties("sgg") { p -> p.keyword { k -> k.eagerGlobalOrdinals(true) } }
                        .properties("emd") { p -> p.keyword { k -> k.eagerGlobalOrdinals(true) } }
                        .properties("land") { p -> landMapping(p) }
                        .properties("building") { p -> buildingMapping(p) }
                        .properties("lastRealEstateTrade") { p -> tradeMapping(p) }
                }
        }
        log.info("[LC] 인덱스 생성: {}", INDEX_NAME)
    }

    private fun landMapping(p: co.elastic.clients.elasticsearch._types.mapping.Property.Builder) =
        p.`object` { o ->
            o.properties("jiyukCd1") { pp -> pp.keyword { it } }
                .properties("jimokCd") { pp -> pp.keyword { it } }
                .properties("area") { pp -> pp.double_ { it } }
                .properties("price") { pp -> pp.long_ { it } }
                .properties("center") { pp -> pp.geoPoint { it } }
        }

    private fun buildingMapping(p: co.elastic.clients.elasticsearch._types.mapping.Property.Builder) =
        p.`object` { o ->
            o.properties("mgmBldrgstPk") { pp -> pp.keyword { it } }
                .properties("mainPurpsCdNm") { pp -> pp.keyword { it } }
                .properties("regstrGbCdNm") { pp -> pp.keyword { it } }
                .properties("pmsDay") { pp -> pp.date { it } }
                .properties("stcnsDay") { pp -> pp.date { it } }
                .properties("useAprDay") { pp -> pp.date { it } }
                .properties("totArea") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }
                .properties("platArea") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }
                .properties("archArea") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }
        }

    private fun tradeMapping(p: co.elastic.clients.elasticsearch._types.mapping.Property.Builder) =
        p.`object` { o ->
            o.properties("property") { pp -> pp.keyword { it } }
                .properties("contractDate") { pp -> pp.date { it } }
                .properties("effectiveAmount") { pp -> pp.long_ { it } }
                .properties("buildingAmountPerM2") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }
                .properties("landAmountPerM2") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }
        }

    private fun bulkIndex(docs: List<LandCompactDocument>) {
        if (docs.isEmpty()) return

        val operations = docs.map { doc ->
            BulkOperation.of { op ->
                op.index { idx ->
                    idx.index(INDEX_NAME)
                        .id(doc.pnu)
                        .document(doc)
                }
            }
        }

        val request = BulkRequest.Builder()
            .operations(operations)
            .build()

        val response = esClient.bulk(request)
        if (response.errors()) {
            val failedCount = response.items().count { it.error() != null }
            log.warn("[LC bulkIndex] 일부 실패: {}/{}", failedCount, docs.size)
        }
    }
}
