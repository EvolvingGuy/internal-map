package com.datahub.geo_poc.es.lbt17x2regionfm

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import com.datahub.geo_poc.jpa.entity.BuildingLedgerOutline
import com.datahub.geo_poc.jpa.entity.BuildingLedgerOutlineSummaries
import com.datahub.geo_poc.jpa.entity.LandCharacteristic
import com.datahub.geo_poc.jpa.entity.RealEstateTrade
import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData
import com.datahub.geo_poc.util.ForcemergeHelper
import com.datahub.geo_poc.util.ParsingUtils
import com.datahub.geo_poc.util.PnuUtils
import com.datahub.geo_poc.jpa.repository.*
import kotlin.streams.asSequence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import com.datahub.geo_poc.util.IndexingLogHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import jakarta.persistence.EntityManager
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap

/**
 * LBT_17x2_REGION_FM (Land Building Trade, 17인덱스 x 2샤드, SD code 기반, Forcemerge) 인덱싱 서비스
 */
@Service
class Lbt17x2regionFmIndexingService(
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
        const val WORKER_COUNT = 10
        const val BATCH_SIZE = 1000
    }

    private fun formatElapsed(ms: Long): String = "${numberFormat.format(ms)}ms (${String.format("%.2f", ms / 1000.0)}s)"
    private fun formatCount(n: Number): String = numberFormat.format(n)
    private fun formatTotalTime(ms: Long): String {
        val seconds = ms / 1000.0
        return if (seconds >= 60) {
            val minutes = seconds / 60
            "${String.format("%.2f", minutes)}m (${String.format("%.1f", seconds)}s)"
        } else {
            "${String.format("%.2f", seconds)}s"
        }
    }

    fun reindex(): Map<String, Any> {
        log.info("[LBT_17x2_REGION_FM] ========== 인덱싱 요청 (비동기, 17인덱스 x 2샤드, SD code) ==========")

        CoroutineScope(indexingDispatcher).launch {
            val startTime = System.currentTimeMillis()

            ensureIndicesExist()
            log.info("[LBT_17x2_REGION_FM] ========== 인덱싱 시작 (17인덱스, 각 2샤드) ==========")

            val totalCount = landCharRepo.countIndexable()
            val expectedBulks = (totalCount + BATCH_SIZE - 1) / BATCH_SIZE
            log.info("[LBT_17x2_REGION_FM] 전체 필지 수: {}건, 예상 벌크 {}회", formatCount(totalCount), formatCount(expectedBulks))

            val emdCodes = landCharRepo.findDistinctEmdCodes()
            val workerEmdMap = emdCodes.withIndex()
                .groupBy { it.index % WORKER_COUNT }
                .mapValues { entry -> entry.value.map { it.value } }

            log.info("[LBT_17x2_REGION_FM] EMD {}개 → {}개 워커로 분배", formatCount(emdCodes.size), workerEmdMap.size)

            val processedCount = AtomicInteger(0)
            val indexedCount = AtomicInteger(0)
            val bulkCount = AtomicInteger(0)
            val buildingCount = AtomicInteger(0)
            val tradeCount = AtomicInteger(0)
            val regionCounts = ConcurrentHashMap<String, AtomicInteger>()
            Lbt17x2regionFmDocument.SD_CODES.forEach { regionCounts[it] = AtomicInteger(0) }

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
                            regionCounts = regionCounts
                        )
                    }
                }
                jobs.awaitAll()
            }

            val elapsed = System.currentTimeMillis() - startTime

            log.info("[LBT_17x2_REGION_FM] ========== 인덱싱 완료 ==========")
            log.info("[LBT_17x2_REGION_FM] 총 문서: {}건, 총 건물: {}건, 총 실거래: {}건, 벌크 {}회, 총 소요시간: {}",
                formatCount(indexedCount.get()), formatCount(buildingCount.get()), formatCount(tradeCount.get()), formatCount(bulkCount.get()), formatTotalTime(elapsed))
            regionCounts.toSortedMap().forEach { (sd, count) ->
                log.info("[LBT_17x2_REGION_FM] 지역 {}: {}건", Lbt17x2regionFmDocument.indexName(sd), formatCount(count.get()))
            }
            log.info("[LBT_17x2_REGION_FM] forcemerge는 수동 호출 필요 (PUT /api/es/lbt-17x2-region-fm/forcemerge)")
        }

        return mapOf(
            "action" to "reindex",
            "status" to "started",
            "async" to true,
            "indexPattern" to Lbt17x2regionFmDocument.allIndexPattern(),
            "indexCount" to Lbt17x2regionFmDocument.INDEX_COUNT,
            "shardsPerIndex" to Lbt17x2regionFmDocument.SHARD_COUNT
        )
    }

    fun forcemerge(): Map<String, Any> {
        ForcemergeHelper.launchAsync(esClient, indexingDispatcher, log, "LBT_17x2_REGION_FM", Lbt17x2regionFmDocument.allIndexNames())
        return mapOf("action" to "forcemerge", "status" to "started", "indices" to Lbt17x2regionFmDocument.allIndexNames())
    }

    fun count(): Map<String, Long> {
        val counts = mutableMapOf<String, Long>()
        for (sd in Lbt17x2regionFmDocument.SD_CODES) {
            val indexName = Lbt17x2regionFmDocument.indexName(sd)
            counts[indexName] = try {
                esClient.count { c -> c.index(indexName) }.count()
            } catch (e: Exception) {
                0L
            }
        }
        counts["total"] = counts.values.sum()
        return counts
    }

    fun deleteIndex(): Map<String, Any> {
        val deleted = mutableListOf<String>()
        for (sd in Lbt17x2regionFmDocument.SD_CODES) {
            val indexName = Lbt17x2regionFmDocument.indexName(sd)
            val exists = try {
                esClient.indices().exists { e -> e.index(indexName) }.value()
            } catch (e: Exception) {
                false
            }
            if (exists) {
                esClient.indices().delete { d -> d.index(indexName) }
                deleted.add(indexName)
            }
        }
        log.info("[LBT_17x2_REGION_FM] 인덱스 삭제 완료: {} ({}개)", deleted, deleted.size)
        return mapOf<String, Any>("deleted" to deleted, "success" to deleted.isNotEmpty())
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
        regionCounts: ConcurrentHashMap<String, AtomicInteger>
    ) {
        val workerStartTime = System.currentTimeMillis()
        var workerIndexed = 0
        var workerBulkCount = 0

        log.info("[LBT_17x2_REGION_FM] Worker-{} 시작: EMD {}개 담당", workerIndex, formatCount(emdCodes.size))

        for ((emdIdx, emdCode) in emdCodes.withIndex()) {
            val (_, emdIndexed, emdBulks, _, _) = processEmd(
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
                regionCounts = regionCounts
            )
            workerIndexed += emdIndexed
            workerBulkCount += emdBulks
        }

        val workerElapsed = System.currentTimeMillis() - workerStartTime
        log.info("[LBT_17x2_REGION_FM] Worker-{} 완료: {}건, 벌크 {}회, {}",
            workerIndex, formatCount(workerIndexed), formatCount(workerBulkCount), formatElapsed(workerElapsed))
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
        regionCounts: ConcurrentHashMap<String, AtomicInteger>
    ): EmdResult {
        var emdProcessed = 0
        var emdIndexed = 0
        var emdBulkCount = 0
        var emdBuildingCount = 0
        var emdTradeCount = 0

        transactionTemplate.execute { _ ->
            landCharRepo.streamByEmdCode(emdCode).use { stream ->
                stream.asSequence()
                    .chunked(BATCH_SIZE.toInt())
                    .forEach { batch ->
                        val stepStartTime = System.currentTimeMillis()
                        val batchResult = processBatch(batch)

                        val bulkStartTime = System.currentTimeMillis()
                        if (batchResult.docs.isNotEmpty()) {
                            bulkIndex(batchResult.docs, regionCounts)
                        }
                        val bulkTime = System.currentTimeMillis() - bulkStartTime
                        val stepTotalTime = System.currentTimeMillis() - stepStartTime

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
                            tag = "LBT_17x2_REGION_FM", workerIndex = workerIndex,
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

        return EmdResult(emdProcessed, emdIndexed, emdBulkCount, emdBuildingCount, emdTradeCount)
    }

    data class BatchResult(
        val docs: List<Lbt17x2regionFmDocument>,
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
            docs = docs, buildingCount = totalBuildingCount, tradeCount = totalTradeCount,
            summariesMs = t2 - t1, outlinesMs = t3 - t2, tradesMs = t4 - t3, docsMs = t5 - t4
        )
    }

    private fun loadBuildingSummaries(buildingPnuList: List<String>): Map<String, List<BuildingLedgerOutlineSummaries>> {
        return try { buildingSummariesRepo.findByPnuIn(buildingPnuList).groupBy { PnuUtils.buildPnuFrom(it) } } catch (e: Exception) { emptyMap() }
    }

    private fun loadBuildingOutlines(buildingPnuList: List<String>): Map<String, List<BuildingLedgerOutline>> {
        return try { buildingOutlineRepo.findByPnuIn(buildingPnuList).groupBy { PnuUtils.buildPnuFrom(it) } } catch (e: Exception) { emptyMap() }
    }

    private fun loadRealEstateTrades(pnuList: List<String>): Map<String, List<RealEstateTradeData>> {
        if (pnuList.isEmpty()) return emptyMap()
        return try {
            realEstateTradeRepo.findAllByPnuIn(pnuList.joinToString(","))
                .groupBy { it.pnu }
                .mapValues { (_, trades) -> trades.map { toRealEstateTradeData(it) } }
        } catch (e: Exception) { emptyMap() }
    }

    private fun createDocument(
        entity: LandCharacteristic,
        summariesMap: Map<String, List<BuildingLedgerOutlineSummaries>>,
        outlineMap: Map<String, List<BuildingLedgerOutline>>,
        tradeMap: Map<String, List<RealEstateTradeData>>
    ): Lbt17x2regionFmDocument? {
        return try {
            val buildingPnu = PnuUtils.convertLandPnuToBuilding(entity.pnu)
            val buildings = mutableListOf<BuildingData>()
            summariesMap[buildingPnu]?.forEach { summary -> toBuildingData(summary)?.let { buildings.add(it) } }
            if (buildings.isEmpty()) { outlineMap[buildingPnu]?.forEach { outline -> toBuildingData(outline)?.let { buildings.add(it) } } }
            val trades = tradeMap[entity.pnu] ?: emptyList()

            Lbt17x2regionFmDocument(
                pnu = entity.pnu, sd = PnuUtils.extractSd(entity.pnu), sgg = PnuUtils.extractSgg(entity.pnu), emd = PnuUtils.extractEmd(entity.pnu),
                land = toLandData(entity), buildings = buildings, trades = trades
            )
        } catch (e: Exception) { log.warn("[LBT_17x2_REGION_FM] 문서 생성 실패 pnu={}: {}", entity.pnu, e.message); null }
    }

    private fun toLandData(entity: LandCharacteristic): Lbt17x2regionFmDocument.Land {
        val center = entity.center
        return Lbt17x2regionFmDocument.Land(
            jiyukCd1 = entity.jiyukCd1?.takeIf { it.isNotBlank() }, jimokCd = entity.jimokCd?.takeIf { it.isNotBlank() },
            area = ParsingUtils.toDoubleOrNull(entity.area), price = ParsingUtils.toLongOrNull(entity.price),
            center = center?.let { mapOf("lat" to it.y, "lon" to it.x) } ?: emptyMap()
        )
    }

    private fun toBuildingData(entity: BuildingLedgerOutlineSummaries): BuildingData? {
        val mgmBldrgstPk = entity.mgmBldrgstPk.takeIf { it.isNotBlank() } ?: return null
        return BuildingData(mgmBldrgstPk = mgmBldrgstPk, mainPurpsCdNm = entity.mainPurpsCdNm?.takeIf { it.isNotBlank() }, regstrGbCdNm = entity.regstrGbCdNm?.takeIf { it.isNotBlank() },
            pmsDay = ParsingUtils.toLocalDateOrNull(entity.pmsDay), stcnsDay = ParsingUtils.toLocalDateOrNull(entity.stcnsDay), useAprDay = ParsingUtils.toLocalDateOrNull(entity.useAprDay),
            totArea = ParsingUtils.toBigDecimalOrNull(entity.totArea), platArea = ParsingUtils.toBigDecimalOrNull(entity.platArea), archArea = ParsingUtils.toBigDecimalOrNull(entity.archArea))
    }

    private fun toBuildingData(entity: BuildingLedgerOutline): BuildingData? {
        val mgmBldrgstPk = entity.mgmBldrgstPk.takeIf { it.isNotBlank() } ?: return null
        return BuildingData(mgmBldrgstPk = mgmBldrgstPk, mainPurpsCdNm = entity.mainPurpsCdNm?.takeIf { it.isNotBlank() }, regstrGbCdNm = entity.regstrGbCdNm?.takeIf { it.isNotBlank() },
            pmsDay = ParsingUtils.toLocalDateOrNull(entity.pmsDay), stcnsDay = ParsingUtils.toLocalDateOrNull(entity.stcnsDay), useAprDay = ParsingUtils.toLocalDateOrNull(entity.useAprDay),
            totArea = ParsingUtils.toBigDecimalOrNull(entity.totArea), platArea = ParsingUtils.toBigDecimalOrNull(entity.platArea), archArea = ParsingUtils.toBigDecimalOrNull(entity.archArea))
    }

    private fun toRealEstateTradeData(entity: RealEstateTrade): RealEstateTradeData {
        return RealEstateTradeData(property = entity.property, contractDate = entity.contractDate, effectiveAmount = entity.effectiveAmount,
            buildingAmountPerM2 = entity.buildingAmountPerNlaM2, landAmountPerM2 = entity.landAmountPerM2)
    }

    private fun ensureIndicesExist() {
        for (sd in Lbt17x2regionFmDocument.SD_CODES) {
            val indexName = Lbt17x2regionFmDocument.indexName(sd)
            val exists = try { esClient.indices().exists { e -> e.index(indexName) }.value() } catch (e: Exception) { false }
            if (exists) { esClient.indices().delete { d -> d.index(indexName) }; log.info("[LBT_17x2_REGION_FM] 기존 인덱스 삭제: {}", indexName) }

            esClient.indices().create { c ->
                c.index(indexName)
                    .settings { s -> s.numberOfShards("${Lbt17x2regionFmDocument.SHARD_COUNT}").numberOfReplicas("0").mapping { m -> m.nestedObjects { n -> n.limit(100000L) } } }
                    .mappings { m ->
                        m.properties("pnu") { pp -> pp.keyword { it } }
                            .properties("sd") { pp -> pp.keyword { k -> k.eagerGlobalOrdinals(true) } }
                            .properties("sgg") { pp -> pp.keyword { k -> k.eagerGlobalOrdinals(true) } }
                            .properties("emd") { pp -> pp.keyword { k -> k.eagerGlobalOrdinals(true) } }
                            .properties("land") { pp -> landMapping(pp) }
                            .properties("buildings") { pp -> buildingsNestedMapping(pp) }
                            .properties("trades") { pp -> tradesNestedMapping(pp) }
                    }
            }
            log.info("[LBT_17x2_REGION_FM] 인덱스 생성: {} (샤드: {})", indexName, Lbt17x2regionFmDocument.SHARD_COUNT)
        }
    }

    private fun landMapping(p: org.opensearch.client.opensearch._types.mapping.Property.Builder) =
        p.`object` { o -> o.properties("jiyukCd1") { pp -> pp.keyword { it } }.properties("jimokCd") { pp -> pp.keyword { it } }.properties("area") { pp -> pp.double_ { it } }.properties("price") { pp -> pp.long_ { it } }.properties("center") { pp -> pp.geoPoint { it } } }

    private fun buildingsNestedMapping(p: org.opensearch.client.opensearch._types.mapping.Property.Builder) =
        p.nested { n -> n.properties("mgmBldrgstPk") { pp -> pp.keyword { it } }.properties("mainPurpsCdNm") { pp -> pp.keyword { it } }.properties("regstrGbCdNm") { pp -> pp.keyword { it } }
            .properties("pmsDay") { pp -> pp.date { it } }.properties("stcnsDay") { pp -> pp.date { it } }.properties("useAprDay") { pp -> pp.date { it } }
            .properties("totArea") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }.properties("platArea") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }.properties("archArea") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } } }

    private fun tradesNestedMapping(p: org.opensearch.client.opensearch._types.mapping.Property.Builder) =
        p.nested { n -> n.properties("property") { pp -> pp.keyword { it } }.properties("contractDate") { pp -> pp.date { it } }.properties("effectiveAmount") { pp -> pp.long_ { it } }
            .properties("buildingAmountPerM2") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } }.properties("landAmountPerM2") { pp -> pp.scaledFloat { sf -> sf.scalingFactor(100.0) } } }

    private fun bulkIndex(docs: List<Lbt17x2regionFmDocument>, regionCounts: ConcurrentHashMap<String, AtomicInteger>) {
        if (docs.isEmpty()) return

        val byRegion = docs.groupBy { it.sd }

        for ((sd, regionDocs) in byRegion) {
            val indexName = Lbt17x2regionFmDocument.indexName(sd)
            regionCounts[sd]?.addAndGet(regionDocs.size)

            val operations = regionDocs.map { doc ->
                BulkOperation.of { op -> op.index { idx -> idx.index(indexName).id(doc.pnu).document(doc) } }
            }

            val request = BulkRequest.Builder().operations(operations).build()
            val response = esClient.bulk(request)
            if (response.errors()) {
                val failedItems = response.items().filter { it.error() != null }
                log.warn("[LBT_17x2_REGION_FM bulkIndex] {} 일부 실패: {}/{}", indexName, failedItems.size, regionDocs.size)
                failedItems.take(3).forEach { item -> log.warn("[LBT_17x2_REGION_FM] 실패 id={}, reason={}", item.id(), item.error()?.reason()) }
            }
        }
    }
}
