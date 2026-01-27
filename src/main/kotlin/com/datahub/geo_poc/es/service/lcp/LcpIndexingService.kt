package com.datahub.geo_poc.es.service.lcp

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import com.datahub.geo_poc.jpa.entity.BuildingLedgerOutline
import com.datahub.geo_poc.jpa.entity.BuildingLedgerOutlineSummaries
import com.datahub.geo_poc.jpa.entity.LandCharacteristic
import com.datahub.geo_poc.jpa.entity.RealEstateTrade
import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData
import com.datahub.geo_poc.es.document.land.LcpDocument
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
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import jakarta.persistence.EntityManager
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * LCP (Land Compact Point) 인덱싱 서비스
 * geometry 없이 center(geo_point)만 저장하여 용량 최소화
 */
@Service
class LcpIndexingService(
    private val landCharRepo: LandCharacteristicRepository,
    private val buildingSummariesRepo: BuildingLedgerOutlineSummariesRepository,
    private val buildingOutlineRepo: BuildingLedgerOutlineRepository,
    private val realEstateTradeRepo: RealEstateTradeRepository,
    private val esClient: ElasticsearchClient,
    private val indexingDispatcher: CoroutineDispatcher,
    private val transactionTemplate: TransactionTemplate,
    private val entityManager: EntityManager
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    companion object {
        const val INDEX_NAME = LcpDocument.INDEX_NAME
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

    // processBatch 결과 (타이밍 포함)
    data class BatchResult(
        val docs: List<LcpDocument>,
        val summariesMs: Long,
        val outlinesMs: Long,
        val tradesMs: Long,
        val docsMs: Long
    )

    fun reindex(): Map<String, Any> = runBlocking {
        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, Any>()

        ensureIndexExists()
        log.info("[LCP] ========== 인덱싱 시작 ==========")

        val totalCount = landCharRepo.countIndexable()
        val expectedBulks = (totalCount + BATCH_SIZE - 1) / BATCH_SIZE
        log.info("[LCP] 전체 필지 수: {}건, 예상 벌크 {}회", formatCount(totalCount), formatCount(expectedBulks))

        val emdCodes = landCharRepo.findDistinctEmdCodes()
        val workerEmdMap = emdCodes.withIndex()
            .groupBy { it.index % WORKER_COUNT }
            .mapValues { entry -> entry.value.map { it.value } }

        log.info("[LCP] EMD {}개 → {}개 워커로 분배", formatCount(emdCodes.size), workerEmdMap.size)

        val processedCount = AtomicInteger(0)
        val indexedCount = AtomicInteger(0)
        val bulkCount = AtomicInteger(0)
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
                        timingStats = timingStats
                    )
                }
            }
            jobs.awaitAll()
        }

        val elapsed = System.currentTimeMillis() - startTime
        val finalBulkCount = bulkCount.get()

        log.info("[LCP] ========== 인덱싱 완료 ==========")
        log.info("[LCP] 총 문서: {}건, 벌크 {}회, 총 소요시간: {}",
            formatCount(indexedCount.get()), formatCount(finalBulkCount), formatTotalTime(elapsed))
        log.info("[LCP] ========== 평균 소요시간 (벌크 {}회, 워커 {}개 기준) ==========", formatCount(finalBulkCount), WORKER_COUNT)
        log.info("[LCP]   summaries 조회: {} (총: {})", formatAvg(timingStats.summariesTime.get(), finalBulkCount), formatTotalTime(timingStats.summariesTime.get() / WORKER_COUNT))
        log.info("[LCP]   outlines 조회:  {} (총: {})", formatAvg(timingStats.outlinesTime.get(), finalBulkCount), formatTotalTime(timingStats.outlinesTime.get() / WORKER_COUNT))
        log.info("[LCP]   trades 조회:    {} (총: {})", formatAvg(timingStats.tradesTime.get(), finalBulkCount), formatTotalTime(timingStats.tradesTime.get() / WORKER_COUNT))
        log.info("[LCP]   docs 생성:      {} (총: {})", formatAvg(timingStats.docsTime.get(), finalBulkCount), formatTotalTime(timingStats.docsTime.get() / WORKER_COUNT))
        log.info("[LCP]   bulk 인덱싱:    {} (총: {})", formatAvg(timingStats.bulkTime.get(), finalBulkCount), formatTotalTime(timingStats.bulkTime.get() / WORKER_COUNT))
        log.info("[LCP]   스텝 총합:      {} (총: {})", formatAvg(timingStats.stepTotalTime.get(), finalBulkCount), formatTotalTime(timingStats.stepTotalTime.get() / WORKER_COUNT))

        // Forcemerge 비동기 실행
        log.info("[LCP] ========== Forcemerge 시작 (비동기) ==========")
        val forcemergeStartTime = System.currentTimeMillis()
        launch(indexingDispatcher) {
            try {
                esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
                val forcemergeElapsed = System.currentTimeMillis() - forcemergeStartTime
                log.info("[LCP] Forcemerge 완료: {}", formatElapsed(forcemergeElapsed))
            } catch (e: Exception) {
                val forcemergeElapsed = System.currentTimeMillis() - forcemergeStartTime
                log.info("[LCP] Forcemerge 요청 완료 (ES 백그라운드 처리 중): {}, 경과: {}", e.message, formatElapsed(forcemergeElapsed))
            }
        }

        results["totalCount"] = totalCount
        results["processed"] = processedCount.get()
        results["indexed"] = indexedCount.get()
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
        log.info("[LCP] forcemerge 시작 (백그라운드)...")
        try {
            esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
            log.info("[LCP] forcemerge 완료")
        } catch (e: Exception) {
            log.info("[LCP] forcemerge 요청 완료 (ES 백그라운드 처리 중): {}", e.message)
        }

        return mapOf(
            "action" to "forcemerge",
            "success" to true
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
            log.info("[LCP] 인덱스 삭제 완료: {}", INDEX_NAME)
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
        timingStats: TimingStats
    ) {
        val workerStartTime = System.currentTimeMillis()
        var workerProcessed = 0
        var workerIndexed = 0
        var workerBulkCount = 0

        log.info("[LCP] Worker-{} 시작: EMD {}개 담당", workerIndex, formatCount(emdCodes.size))

        for ((emdIdx, emdCode) in emdCodes.withIndex()) {
            val (emdProcessed, emdIndexed, emdBulks) = processEmd(
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
                timingStats = timingStats
            )
            workerProcessed += emdProcessed
            workerIndexed += emdIndexed
            workerBulkCount += emdBulks
        }

        val workerElapsed = System.currentTimeMillis() - workerStartTime
        log.info("[LCP] Worker-{} 완료: {}건, 벌크 {}회, {}",
            workerIndex, formatCount(workerIndexed), formatCount(workerBulkCount), formatElapsed(workerElapsed))
    }

    data class EmdResult(val processed: Int, val indexed: Int, val bulks: Int)

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
        timingStats: TimingStats
    ): EmdResult {
        var emdProcessed = 0
        var emdIndexed = 0
        var emdBulkCount = 0
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

                        val globalProcessed = processedCount.addAndGet(batch.size)
                        indexedCount.addAndGet(batchResult.docs.size)
                        val globalBulkCount = bulkCount.incrementAndGet()

                        val elapsed = System.currentTimeMillis() - globalStartTime
                        val percent = String.format("%.1f", globalProcessed * 100.0 / totalCount)

                        log.info("[LCP] Worker-{} 벌크 #{}/{}: {}/{} ({}%) EMD={} ({}/{}), 스텝 {}, 누적 {}",
                            workerIndex,
                            formatCount(globalBulkCount), formatCount(expectedBulks),
                            formatCount(globalProcessed), formatCount(totalCount), percent,
                            emdCode, emdIdx + 1, totalEmd,
                            formatElapsed(stepTotalTime), formatTotalTime(elapsed))

                        entityManager.clear()
                    }
            }
        }

        val emdElapsed = System.currentTimeMillis() - emdStartTime
        log.info("[LCP] Worker-{} EMD {} 완료: {}건, 벌크 {}회, {}",
            workerIndex, emdCode, formatCount(emdProcessed), formatCount(emdBulkCount), formatElapsed(emdElapsed))

        return EmdResult(emdProcessed, emdIndexed, emdBulkCount)
    }

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

        val docs = entities.mapNotNull { entity ->
            createDocument(entity, summariesMap, outlineMap, tradeMap)
        }
        val t5 = System.currentTimeMillis()

        return BatchResult(
            docs = docs,
            summariesMs = t2 - t1,
            outlinesMs = t3 - t2,
            tradesMs = t4 - t3,
            docsMs = t5 - t4
        )
    }

    private fun loadBuildingSummaries(buildingPnuList: List<String>): Map<String, BuildingLedgerOutlineSummaries> {
        return try {
            buildingSummariesRepo.findByPnuIn(buildingPnuList)
                .groupBy { PnuUtils.buildPnuFrom(it) }
                .mapValues { it.value.first() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun loadBuildingOutlines(buildingPnuList: List<String>): Map<String, BuildingLedgerOutline> {
        return try {
            buildingOutlineRepo.findByPnuIn(buildingPnuList)
                .groupBy { PnuUtils.buildPnuFrom(it) }
                .mapValues { it.value.first() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun loadRealEstateTrades(pnuList: List<String>): Map<String, RealEstateTradeData> {
        if (pnuList.isEmpty()) return emptyMap()
        return try {
            realEstateTradeRepo.findLatestByPnuIn(pnuList.joinToString(","))
                .associate { it.pnu to toRealEstateTradeData(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun createDocument(
        entity: LandCharacteristic,
        summariesMap: Map<String, BuildingLedgerOutlineSummaries>,
        outlineMap: Map<String, BuildingLedgerOutline>,
        tradeMap: Map<String, RealEstateTradeData>
    ): LcpDocument? {
        val center = entity.center ?: return null  // center 필수

        return try {
            val buildingPnu = PnuUtils.convertLandPnuToBuilding(entity.pnu)

            val buildingData = summariesMap[buildingPnu]?.let { toBuildingData(it) }
                ?: outlineMap[buildingPnu]?.let { toBuildingData(it) }

            val tradeData = tradeMap[entity.pnu]

            LcpDocument(
                pnu = entity.pnu,
                sd = PnuUtils.extractSd(entity.pnu),
                sgg = PnuUtils.extractSgg(entity.pnu),
                emd = PnuUtils.extractEmd(entity.pnu),
                land = LcpDocument.Land(
                    jiyukCd1 = entity.jiyukCd1?.takeIf { it.isNotBlank() },
                    jimokCd = entity.jimokCd?.takeIf { it.isNotBlank() },
                    area = ParsingUtils.toDoubleOrNull(entity.area),
                    price = ParsingUtils.toLongOrNull(entity.price),
                    center = mapOf("lat" to center.y, "lon" to center.x)
                ),
                building = buildingData,
                lastRealEstateTrade = tradeData
            )
        } catch (e: Exception) {
            log.warn("[LCP] 문서 생성 실패 pnu={}: {}", entity.pnu, e.message)
            null
        }
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
            log.info("[LCP] 기존 인덱스 삭제")
        }

        esClient.indices().create { c ->
            c.index(INDEX_NAME)
                .settings { s ->
                    s.numberOfShards("4")
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
        log.info("[LCP] 인덱스 생성: {}", INDEX_NAME)
    }

    private fun landMapping(p: co.elastic.clients.elasticsearch._types.mapping.Property.Builder) =
        p.`object` { o ->
            o.properties("jiyukCd1") { pp -> pp.keyword { it } }
                .properties("jimokCd") { pp -> pp.keyword { it } }
                .properties("area") { pp -> pp.double_ { it } }
                .properties("price") { pp -> pp.long_ { it } }
                .properties("center") { pp -> pp.geoPoint { it } }
                // geometry 없음 - LC와의 핵심 차이점
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

    private fun bulkIndex(docs: List<LcpDocument>) {
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
            log.warn("[LCP bulkIndex] 일부 실패: {}/{}", failedItems.size, docs.size)
            failedItems.take(3).forEach { item ->
                log.warn("[LCP] 실패 id={}, reason={}", item.id(), item.error()?.reason())
            }
        }
    }
}
