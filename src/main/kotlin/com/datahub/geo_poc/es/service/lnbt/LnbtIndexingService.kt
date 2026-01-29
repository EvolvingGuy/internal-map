package com.datahub.geo_poc.es.service.lnbt

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import com.datahub.geo_poc.jpa.entity.BuildingLedgerOutline
import com.datahub.geo_poc.jpa.entity.BuildingLedgerOutlineSummaries
import com.datahub.geo_poc.jpa.entity.LandCharacteristic
import com.datahub.geo_poc.jpa.entity.RealEstateTrade
import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData
import com.datahub.geo_poc.es.document.land.LnbtDocument
import com.datahub.geo_poc.util.GeoJsonUtils
import com.datahub.geo_poc.util.ParsingUtils
import com.datahub.geo_poc.util.PnuUtils
import com.datahub.geo_poc.jpa.repository.*
import kotlin.streams.asSequence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import com.datahub.geo_poc.util.IndexingLogHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import jakarta.persistence.EntityManager
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * LNBT (Land Nested Building Trade) 인덱싱 서비스
 * 건물과 실거래 모두 nested array로 저장
 */
@Service
class LnbtIndexingService(
    private val landCharRepo: LandCharacteristicRepository,
    private val buildingSummariesRepo: BuildingLedgerOutlineSummariesRepository,
    private val buildingOutlineRepo: BuildingLedgerOutlineRepository,
    private val realEstateTradeRepo: RealEstateTradeRepository,
    private val esClient: OpenSearchClient,
    private val indexingDispatcher: CoroutineDispatcher,
    private val transactionTemplate: TransactionTemplate,
    private val entityManager: EntityManager
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    companion object {
        const val INDEX_NAME = LnbtDocument.INDEX_NAME
        const val WORKER_COUNT = 10
        const val BATCH_SIZE = 1000
        const val STREAM_SIZE = "$BATCH_SIZE"
    }

    private fun formatElapsed(ms: Long): String = "${numberFormat.format(ms)}ms (${String.format("%.2f", ms / 1000.0)}s)"
    private fun formatCount(n: Number): String = numberFormat.format(n)
    private fun formatAvg(totalMs: Long, count: Int): String {
        if (count <= 0) return "N/A"
        val avgMs = totalMs / count
        return "${String.format("%.2f", avgMs / 1000.0)}s"
    }
    private fun formatTotalTime(ms: Long): String {
        val seconds = ms / 1000.0
        return if (seconds >= 60) {
            val minutes = seconds / 60
            "${String.format("%.2f", minutes)}m (${String.format("%.1f", seconds)}s)"
        } else {
            "${String.format("%.2f", seconds)}s"
        }
    }

    // 타이밍 통계용 누적 카운터
    data class TimingStats(
        val summariesTime: AtomicLong = AtomicLong(0),
        val outlinesTime: AtomicLong = AtomicLong(0),
        val tradesTime: AtomicLong = AtomicLong(0),
        val docsTime: AtomicLong = AtomicLong(0),
        val bulkTime: AtomicLong = AtomicLong(0),
        val stepTotalTime: AtomicLong = AtomicLong(0)
    )

    fun reindex(): Map<String, Any> = runBlocking {
        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, Any>()

        ensureIndexExists()
        log.info("[LNBT] ========== 인덱싱 시작 ==========")

        val totalCount = landCharRepo.countIndexable()
        val expectedBulks = (totalCount + BATCH_SIZE - 1) / BATCH_SIZE
        log.info("[LNBT] 전체 필지 수: {}건, 예상 벌크 {}회", formatCount(totalCount), formatCount(expectedBulks))

        val emdCodes = landCharRepo.findDistinctEmdCodes()
        val workerEmdMap = emdCodes.withIndex()
            .groupBy { it.index % WORKER_COUNT }
            .mapValues { entry -> entry.value.map { it.value } }

        log.info("[LNBT] EMD {}개 → {}개 워커로 분배", formatCount(emdCodes.size), workerEmdMap.size)

        val processedCount = AtomicInteger(0)
        val indexedCount = AtomicInteger(0)
        val bulkCount = AtomicInteger(0)
        val buildingCount = AtomicInteger(0)
        val tradeCount = AtomicInteger(0)
        val timingStats = TimingStats()

        coroutineScope {
            val jobs = workerEmdMap.map { (workerIndex, myEmdCodes) ->
                async(indexingDispatcher) {
                    processWorker(
                        workerIndex = workerIndex,
                        emdCodes = myEmdCodes,
                        totalCount = totalCount,
                        expectedBulks = expectedBulks,
                        startTime = startTime,
                        processedCount = processedCount,
                        indexedCount = indexedCount,
                        bulkCount = bulkCount,
                        buildingCount = buildingCount,
                        tradeCount = tradeCount,
                        timingStats = timingStats
                    )
                }
            }
            jobs.awaitAll()
        }

        val elapsed = System.currentTimeMillis() - startTime
        val finalBulkCount = bulkCount.get()

        log.info("[LNBT] ========== 인덱싱 완료 ==========")
        log.info("[LNBT] 총 문서: {}건, 총 건물: {}건, 총 실거래: {}건, 벌크 {}회, 총 소요시간: {}",
            formatCount(indexedCount.get()), formatCount(buildingCount.get()), formatCount(tradeCount.get()), formatCount(finalBulkCount), formatTotalTime(elapsed))
        log.info("[LNBT] ========== 평균 소요시간 (벌크 {}회, 워커 {}개 기준) ==========", formatCount(finalBulkCount), WORKER_COUNT)
        log.info("[LNBT]   summaries 조회: {} (총: {})", formatAvg(timingStats.summariesTime.get(), finalBulkCount), formatTotalTime(timingStats.summariesTime.get() / WORKER_COUNT))
        log.info("[LNBT]   outlines 조회:  {} (총: {})", formatAvg(timingStats.outlinesTime.get(), finalBulkCount), formatTotalTime(timingStats.outlinesTime.get() / WORKER_COUNT))
        log.info("[LNBT]   trades 조회:    {} (총: {})", formatAvg(timingStats.tradesTime.get(), finalBulkCount), formatTotalTime(timingStats.tradesTime.get() / WORKER_COUNT))
        log.info("[LNBT]   docs 생성:      {} (총: {})", formatAvg(timingStats.docsTime.get(), finalBulkCount), formatTotalTime(timingStats.docsTime.get() / WORKER_COUNT))
        log.info("[LNBT]   bulk 인덱싱:    {} (총: {})", formatAvg(timingStats.bulkTime.get(), finalBulkCount), formatTotalTime(timingStats.bulkTime.get() / WORKER_COUNT))
        log.info("[LNBT]   스텝 총합:      {} (총: {})", formatAvg(timingStats.stepTotalTime.get(), finalBulkCount), formatTotalTime(timingStats.stepTotalTime.get() / WORKER_COUNT))

        // forcemerge 비활성화: 집계 기반 워크로드에서 실효성 없음
        // log.info("[LNBT] ========== Forcemerge 시작 (비동기) ==========")
        // val forcemergeStartTime = System.currentTimeMillis()
        // launch(indexingDispatcher) {
        //     try {
        //         esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
        //         val forcemergeElapsed = System.currentTimeMillis() - forcemergeStartTime
        //         log.info("[LNBT] Forcemerge 완료: {}", formatElapsed(forcemergeElapsed))
        //     } catch (e: Exception) {
        //         val forcemergeElapsed = System.currentTimeMillis() - forcemergeStartTime
        //         log.info("[LNBT] Forcemerge 요청 완료 (ES 백그라운드 처리 중): {}, 경과: {}", e.message, formatElapsed(forcemergeElapsed))
        //     }
        // }

        results["totalCount"] = totalCount
        results["processed"] = processedCount.get()
        results["indexed"] = indexedCount.get()
        results["buildingCount"] = buildingCount.get()
        results["tradeCount"] = tradeCount.get()
        results["bulkCount"] = finalBulkCount
        results["elapsedMs"] = elapsed
        results["success"] = true

        results
    }

    fun count(): Long {
        return try {
            esClient.count { c -> c.index(INDEX_NAME) }.count()
        } catch (e: Exception) {
            0
        }
    }

    fun forcemerge(): Map<String, Any> {
        // forcemerge 비활성화: 집계 기반 워크로드에서 실효성 없음
        // log.info("[LNBT] forcemerge 시작 (백그라운드)...")
        // try {
        //     esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
        //     log.info("[LNBT] forcemerge 완료")
        // } catch (e: Exception) {
        //     log.info("[LNBT] forcemerge 요청 완료 (ES 백그라운드 처리 중): {}", e.message)
        // }
        return mapOf(
            "action" to "forcemerge",
            "status" to "disabled",
            "reason" to "집계 기반 워크로드에서 실효성 없음"
        )
    }

    fun deleteIndex(): Map<String, Any> {
        val exists = try {
            esClient.indices().exists { e -> e.index(INDEX_NAME) }.value()
        } catch (e: Exception) {
            false
        }

        return if (exists) {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            log.info("[LNBT] 인덱스 삭제 완료: {}", INDEX_NAME)
            mapOf("deleted" to true, "index" to INDEX_NAME)
        } else {
            mapOf("deleted" to false, "index" to INDEX_NAME, "reason" to "not exists")
        }
    }

    // ==================== Private ====================

    private fun processWorker(
        workerIndex: Int,
        emdCodes: List<String>,
        totalCount: Long,
        expectedBulks: Long,
        startTime: Long,
        processedCount: AtomicInteger,
        indexedCount: AtomicInteger,
        bulkCount: AtomicInteger,
        buildingCount: AtomicInteger,
        tradeCount: AtomicInteger,
        timingStats: TimingStats
    ) {
        val workerStartTime = System.currentTimeMillis()
        var workerProcessed = 0
        var workerIndexed = 0
        var workerBulkCount = 0
        var workerBuildingCount = 0
        var workerTradeCount = 0

        log.info("[LNBT] Worker-{} 시작: EMD {}개 담당", workerIndex, formatCount(emdCodes.size))

        for ((emdIdx, emdCode) in emdCodes.withIndex()) {
            val (emdProcessed, emdIndexed, emdBulks, emdBuildings, emdTrades) = processEmd(
                emdCode = emdCode,
                workerIndex = workerIndex,
                emdIdx = emdIdx,
                totalEmd = emdCodes.size,
                totalCount = totalCount,
                expectedBulks = expectedBulks,
                globalStartTime = startTime,
                processedCount = processedCount,
                indexedCount = indexedCount,
                bulkCount = bulkCount,
                buildingCount = buildingCount,
                tradeCount = tradeCount,
                timingStats = timingStats
            )
            workerProcessed += emdProcessed
            workerIndexed += emdIndexed
            workerBulkCount += emdBulks
            workerBuildingCount += emdBuildings
            workerTradeCount += emdTrades
        }

        val workerElapsed = System.currentTimeMillis() - workerStartTime
        log.info("[LNBT] Worker-{} 완료: {}건, 건물 {}건, 실거래 {}건, 벌크 {}회, {}",
            workerIndex, formatCount(workerIndexed), formatCount(workerBuildingCount), formatCount(workerTradeCount), formatCount(workerBulkCount), formatElapsed(workerElapsed))
    }

    data class EmdResult(val processed: Int, val indexed: Int, val bulks: Int, val buildings: Int, val trades: Int)

    private fun processEmd(
        emdCode: String,
        workerIndex: Int,
        emdIdx: Int,
        totalEmd: Int,
        totalCount: Long,
        expectedBulks: Long,
        globalStartTime: Long,
        processedCount: AtomicInteger,
        indexedCount: AtomicInteger,
        bulkCount: AtomicInteger,
        buildingCount: AtomicInteger,
        tradeCount: AtomicInteger,
        timingStats: TimingStats
    ): EmdResult {
        var emdProcessed = 0
        var emdIndexed = 0
        var emdBulkCount = 0
        var emdBuildingCount = 0
        var emdTradeCount = 0
        val emdStartTime = System.currentTimeMillis()

        transactionTemplate.execute { _ ->
            landCharRepo.streamByEmdCode(emdCode).use { stream ->
                stream.asSequence()
                    .chunked(BATCH_SIZE.toInt())
                    .forEach { batch ->
                        val stepStartTime = System.currentTimeMillis()
                        val batchResult = processBatch(batch)

                        val bulkStartTime = System.currentTimeMillis()
                        if (batchResult.docs.isNotEmpty()) {
                            bulkIndex(batchResult.docs)
                        }
                        val bulkTime = System.currentTimeMillis() - bulkStartTime
                        val stepTotalTime = System.currentTimeMillis() - stepStartTime

                        // 타이밍 누적
                        timingStats.summariesTime.addAndGet(batchResult.summariesMs)
                        timingStats.outlinesTime.addAndGet(batchResult.outlinesMs)
                        timingStats.tradesTime.addAndGet(batchResult.tradesMs)
                        timingStats.docsTime.addAndGet(batchResult.docsMs)
                        timingStats.bulkTime.addAndGet(bulkTime)
                        timingStats.stepTotalTime.addAndGet(stepTotalTime)

                        emdProcessed += batch.size
                        emdIndexed += batchResult.docs.size
                        emdBulkCount++
                        emdBuildingCount += batchResult.buildingCount
                        emdTradeCount += batchResult.tradeCount

                        val globalProcessed = processedCount.addAndGet(batch.size)
                        indexedCount.addAndGet(batchResult.docs.size)
                        val globalBulkCount = bulkCount.incrementAndGet()
                        buildingCount.addAndGet(batchResult.buildingCount)
                        tradeCount.addAndGet(batchResult.tradeCount)

                        val elapsed = System.currentTimeMillis() - globalStartTime

                        IndexingLogHelper.logBulkStep(log, IndexingLogHelper.BulkStepLog(
                            tag = "LNBT", workerIndex = workerIndex,
                            bulkCount = globalBulkCount, expectedBulks = expectedBulks,
                            processed = globalProcessed, totalCount = totalCount,
                            emdCode = emdCode, emdIdx = emdIdx, totalEmd = totalEmd,
                            summariesMs = batchResult.summariesMs, outlinesMs = batchResult.outlinesMs,
                            tradesMs = batchResult.tradesMs, docsMs = batchResult.docsMs,
                            bulkMs = bulkTime, stepTotalMs = stepTotalTime, accumulatedMs = elapsed,
                            buildingCount = batchResult.buildingCount, tradeCount = batchResult.tradeCount
                        ))

                        entityManager.clear()
                    }
            }
        }

        val emdElapsed = System.currentTimeMillis() - emdStartTime
        log.info("[LNBT] Worker-{} EMD {} 완료: {}건, 건물 {}건, 실거래 {}건, 벌크 {}회, {}",
            workerIndex, emdCode, formatCount(emdProcessed), formatCount(emdBuildingCount), formatCount(emdTradeCount), formatCount(emdBulkCount), formatElapsed(emdElapsed))

        return EmdResult(emdProcessed, emdIndexed, emdBulkCount, emdBuildingCount, emdTradeCount)
    }

    // processBatch 결과 (타이밍 포함)
    data class BatchResult(
        val docs: List<LnbtDocument>,
        val buildingCount: Int,
        val tradeCount: Int,
        val summariesMs: Long,
        val outlinesMs: Long,
        val tradesMs: Long,
        val docsMs: Long
    )

    private fun processBatch(entities: List<LandCharacteristic>): BatchResult {
        val pnuList = entities.map { it.pnu }
        val buildingPnuList = pnuList.map { PnuUtils.convertLandPnuToBuilding(it) }

        val t1 = System.currentTimeMillis()
        val summariesMap = loadBuildingSummaries(buildingPnuList)
        val t2 = System.currentTimeMillis()
        val outlineMap = loadBuildingOutlines(buildingPnuList)
        val t3 = System.currentTimeMillis()
        val tradeMap = loadRealEstateTrades(pnuList)
        val t4 = System.currentTimeMillis()

        var totalBuildingCount = 0
        var totalTradeCount = 0
        val docs = entities.mapNotNull { entity ->
            val doc = createDocument(entity, summariesMap, outlineMap, tradeMap)
            if (doc != null) {
                totalBuildingCount += doc.buildings.size
                totalTradeCount += doc.trades.size
            }
            doc
        }
        val t5 = System.currentTimeMillis()

        return BatchResult(
            docs = docs,
            buildingCount = totalBuildingCount,
            tradeCount = totalTradeCount,
            summariesMs = t2 - t1,
            outlinesMs = t3 - t2,
            tradesMs = t4 - t3,
            docsMs = t5 - t4
        )
    }

    private fun loadBuildingSummaries(buildingPnuList: List<String>): Map<String, List<BuildingLedgerOutlineSummaries>> {
        return try {
            buildingSummariesRepo.findByPnuIn(buildingPnuList)
                .groupBy { PnuUtils.buildPnuFrom(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun loadBuildingOutlines(buildingPnuList: List<String>): Map<String, List<BuildingLedgerOutline>> {
        return try {
            buildingOutlineRepo.findByPnuIn(buildingPnuList)
                .groupBy { PnuUtils.buildPnuFrom(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // 전체 실거래 조회 (N건)
    private fun loadRealEstateTrades(pnuList: List<String>): Map<String, List<RealEstateTradeData>> {
        if (pnuList.isEmpty()) return emptyMap()
        return try {
            realEstateTradeRepo.findAllByPnuIn(pnuList.joinToString(","))
                .groupBy { it.pnu }
                .mapValues { (_, trades) -> trades.map { toRealEstateTradeData(it) } }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun createDocument(
        entity: LandCharacteristic,
        summariesMap: Map<String, List<BuildingLedgerOutlineSummaries>>,
        outlineMap: Map<String, List<BuildingLedgerOutline>>,
        tradeMap: Map<String, List<RealEstateTradeData>>
    ): LnbtDocument? {
        return try {
            val buildingPnu = PnuUtils.convertLandPnuToBuilding(entity.pnu)

            // 건물 리스트 생성 (summaries 우선, 없으면 outline)
            val buildings = mutableListOf<BuildingData>()

            summariesMap[buildingPnu]?.forEach { summary ->
                toBuildingData(summary)?.let { buildings.add(it) }
            }

            // summaries가 없는 경우 outline에서 가져옴
            if (buildings.isEmpty()) {
                outlineMap[buildingPnu]?.forEach { outline ->
                    toBuildingData(outline)?.let { buildings.add(it) }
                }
            }

            // 실거래 리스트 (N건)
            val trades = tradeMap[entity.pnu] ?: emptyList()

            LnbtDocument(
                pnu = entity.pnu,
                sd = PnuUtils.extractSd(entity.pnu),
                sgg = PnuUtils.extractSgg(entity.pnu),
                emd = PnuUtils.extractEmd(entity.pnu),
                land = toLandData(entity),
                buildings = buildings,
                trades = trades
            )
        } catch (e: Exception) {
            log.warn("[LNBT] 문서 생성 실패 pnu={}: {}", entity.pnu, e.message)
            null
        }
    }

    private fun toLandData(entity: LandCharacteristic): LnbtDocument.Land {
        val center = entity.center
        val geometryObj = GeoJsonUtils.toGeoJson(entity.geometry)

        return LnbtDocument.Land(
            jiyukCd1 = entity.jiyukCd1?.takeIf { it.isNotBlank() },
            jimokCd = entity.jimokCd?.takeIf { it.isNotBlank() },
            area = ParsingUtils.toDoubleOrNull(entity.area),
            price = ParsingUtils.toLongOrNull(entity.price),
            center = center?.let { mapOf("lat" to it.y, "lon" to it.x) } ?: emptyMap(),
            geometry = geometryObj
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

    private fun toRealEstateTradeData(entity: RealEstateTrade): RealEstateTradeData {
        return RealEstateTradeData(
            property = entity.property,
            contractDate = entity.contractDate,
            effectiveAmount = entity.effectiveAmount,
            buildingAmountPerM2 = entity.buildingAmountPerNlaM2,
            landAmountPerM2 = entity.landAmountPerM2
        )
    }

    private fun ensureIndexExists() {
        val exists = try {
            esClient.indices().exists { e -> e.index(INDEX_NAME) }.value()
        } catch (e: Exception) {
            false
        }

        if (exists) {
            esClient.indices().delete { d -> d.index(INDEX_NAME) }
            log.info("[LNBT] 기존 인덱스 삭제")
        }

        esClient.indices().create { c ->
            c.index(INDEX_NAME)
                .settings { s ->
                    s.numberOfShards("4")
                        .numberOfReplicas("0")
                        .mapping { m -> m.nestedObjects { n -> n.limit(100000L) } }
                }
                .mappings { m ->
                    m.properties("pnu") { p -> p.keyword { it } }
                        .properties("sd") { p -> p.keyword { k -> k.eagerGlobalOrdinals(true) } }
                        .properties("sgg") { p -> p.keyword { k -> k.eagerGlobalOrdinals(true) } }
                        .properties("emd") { p -> p.keyword { k -> k.eagerGlobalOrdinals(true) } }
                        .properties("land") { p -> landMapping(p) }
                        .properties("buildings") { p -> buildingsNestedMapping(p) }
                        .properties("trades") { p -> tradesNestedMapping(p) }
                }
        }
        log.info("[LNBT] 인덱스 생성: {}", INDEX_NAME)
    }

    private fun landMapping(p: org.opensearch.client.opensearch._types.mapping.Property.Builder) =
        p.`object` { o ->
            o.properties("jiyukCd1") { pp -> pp.keyword { it } }
                .properties("jimokCd") { pp -> pp.keyword { it } }
                .properties("area") { pp -> pp.double_ { it } }
                .properties("price") { pp -> pp.long_ { it } }
                .properties("center") { pp -> pp.geoPoint { it } }
                .properties("geometry") { pp -> pp.geoShape { it } }
        }

    // nested 타입으로 매핑 - 개별 건물 단위 쿼리 지원
    private fun buildingsNestedMapping(p: org.opensearch.client.opensearch._types.mapping.Property.Builder) =
        p.nested { n ->
            n.properties("mgmBldrgstPk") { pp -> pp.keyword { it } }
                .properties("mainPurpsCdNm") { pp -> pp.keyword { it } }
                .properties("regstrGbCdNm") { pp -> pp.keyword { it } }
                .properties("pmsDay") { pp -> pp.date { it } }
                .properties("stcnsDay") { pp -> pp.date { it } }
                .properties("useAprDay") { pp -> pp.date { it } }
                .properties("totArea") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }
                .properties("platArea") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }
                .properties("archArea") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }
        }

    // nested 타입으로 매핑 - 개별 실거래 단위 쿼리 지원
    private fun tradesNestedMapping(p: org.opensearch.client.opensearch._types.mapping.Property.Builder) =
        p.nested { n ->
            n.properties("property") { pp -> pp.keyword { it } }
                .properties("contractDate") { pp -> pp.date { it } }
                .properties("effectiveAmount") { pp -> pp.long_ { it } }
                .properties("buildingAmountPerM2") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }
                .properties("landAmountPerM2") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }
        }

    private fun bulkIndex(docs: List<LnbtDocument>) {
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
            val failedItems = response.items().filter { it.error() != null }
            log.warn("[LNBT bulkIndex] 일부 실패: {}/{}", failedItems.size, docs.size)
            failedItems.take(3).forEach { item ->
                log.warn("[LNBT] 실패 id={}, reason={}", item.id(), item.error()?.reason())
            }
        }
    }
}
