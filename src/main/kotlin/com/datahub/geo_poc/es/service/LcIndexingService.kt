package com.datahub.geo_poc.es.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import com.datahub.geo_poc.jpa.entity.BuildingLedgerOutline
import com.datahub.geo_poc.jpa.entity.BuildingLedgerOutlineSummaries
import com.datahub.geo_poc.jpa.entity.LandCharacteristic
import com.datahub.geo_poc.jpa.entity.RealEstateTrade
import com.datahub.geo_poc.es.document.common.BuildingData
import com.datahub.geo_poc.es.document.common.RealEstateTradeData
import com.datahub.geo_poc.es.document.land.LandCompactDocument
import com.datahub.geo_poc.util.GeoJsonUtils
import com.datahub.geo_poc.util.ParsingUtils
import com.datahub.geo_poc.util.PnuUtils
import com.datahub.geo_poc.jpa.repository.*
import kotlin.streams.asSequence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import jakarta.persistence.EntityManager
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * LC (Land Compact) 인덱싱 서비스
 * geometry를 geo_shape로 저장하여 intersects 쿼리 지원
 */
@Service
class LcIndexingService(
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
        const val INDEX_NAME = LandCompactDocument.INDEX_NAME
        const val WORKER_COUNT = 20
        const val BATCH_SIZE = 5000
    }

    private fun formatElapsed(ms: Long): String = "${numberFormat.format(ms)}ms (${String.format("%.2f", ms / 1000.0)}s)"
    private fun formatCount(n: Number): String = numberFormat.format(n)

    fun reindex(): Map<String, Any> = runBlocking {
        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, Any>()

        ensureIndexExists()
        log.info("[LC] ========== 인덱싱 시작 ==========")

        val totalCount = landCharRepo.countIndexable()
        val expectedBulks = (totalCount + BATCH_SIZE - 1) / BATCH_SIZE
        log.info("[LC] 전체 필지 수: {}건, 예상 벌크 {}회", formatCount(totalCount), formatCount(expectedBulks))

        // SGG 코드를 워커에 라운드로빈 분배
        val sggCodes = landCharRepo.findDistinctSggCodes()
        val workerSggMap = sggCodes.withIndex()
            .groupBy { it.index % WORKER_COUNT }
            .mapValues { entry -> entry.value.map { it.value } }

        log.info("[LC] SGG {}개 → {}개 워커로 분배", formatCount(sggCodes.size), workerSggMap.size)

        val processedCount = AtomicInteger(0)
        val indexedCount = AtomicInteger(0)
        val bulkCount = AtomicInteger(0)

        coroutineScope {
            val jobs = workerSggMap.map { (workerIndex, mySggCodes) ->
                async(indexingDispatcher) {
                    processWorker(
                        workerIndex = workerIndex,
                        sggCodes = mySggCodes,
                        totalCount = totalCount,
                        expectedBulks = expectedBulks,
                        startTime = startTime,
                        processedCount = processedCount,
                        indexedCount = indexedCount,
                        bulkCount = bulkCount
                    )
                }
            }
            jobs.awaitAll()
        }

        log.info("[LC] ========== Forcemerge 시작 ==========")
        val forcemergeStart = System.currentTimeMillis()
        esClient.indices().forcemerge { f -> f.index(INDEX_NAME).maxNumSegments(1L) }
        log.info("[LC] Forcemerge 완료: {}", formatElapsed(System.currentTimeMillis() - forcemergeStart))

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LC] ========== 인덱싱 완료 ==========")
        log.info("[LC] 총 문서: {}건, 벌크 {}회, 총 소요시간: {}",
            formatCount(indexedCount.get()), formatCount(bulkCount.get()), formatElapsed(elapsed))

        results["totalCount"] = totalCount
        results["processed"] = processedCount.get()
        results["indexed"] = indexedCount.get()
        results["bulkCount"] = bulkCount.get()
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
            mapOf("deleted" to false, "index" to INDEX_NAME, "reason" to "not exists")
        }
    }

    // ==================== Private ====================

    private fun processWorker(
        workerIndex: Int,
        sggCodes: List<String>,
        totalCount: Long,
        expectedBulks: Long,
        startTime: Long,
        processedCount: AtomicInteger,
        indexedCount: AtomicInteger,
        bulkCount: AtomicInteger
    ) {
        val workerStartTime = System.currentTimeMillis()
        var workerProcessed = 0
        var workerIndexed = 0
        var workerBulkCount = 0

        log.info("[LC] Worker-{} 시작: SGG {}개 담당", workerIndex, formatCount(sggCodes.size))

        for ((sggIdx, sggCode) in sggCodes.withIndex()) {
            val (sggProcessed, sggIndexed, sggBulks) = processSgg(
                sggCode = sggCode,
                workerIndex = workerIndex,
                sggIdx = sggIdx,
                totalSgg = sggCodes.size,
                totalCount = totalCount,
                expectedBulks = expectedBulks,
                globalStartTime = startTime,
                processedCount = processedCount,
                indexedCount = indexedCount,
                bulkCount = bulkCount
            )
            workerProcessed += sggProcessed
            workerIndexed += sggIndexed
            workerBulkCount += sggBulks
        }

        val workerElapsed = System.currentTimeMillis() - workerStartTime
        log.info("[LC] Worker-{} 완료: {}건, 벌크 {}회, {}",
            workerIndex, formatCount(workerIndexed), formatCount(workerBulkCount), formatElapsed(workerElapsed))
    }

    private fun processSgg(
        sggCode: String,
        workerIndex: Int,
        sggIdx: Int,
        totalSgg: Int,
        totalCount: Long,
        expectedBulks: Long,
        globalStartTime: Long,
        processedCount: AtomicInteger,
        indexedCount: AtomicInteger,
        bulkCount: AtomicInteger
    ): Triple<Int, Int, Int> {
        var sggProcessed = 0
        var sggIndexed = 0
        var sggBulkCount = 0
        val sggStartTime = System.currentTimeMillis()

        transactionTemplate.execute { _ ->
            landCharRepo.streamBySggCode(sggCode).use { stream ->
                stream.asSequence()
                    .chunked(BATCH_SIZE)
                    .forEach { batch ->
                        val bulkStartTime = System.currentTimeMillis()
                        val docs = processBatch(batch)

                        if (docs.isNotEmpty()) {
                            bulkIndex(docs)
                        }

                        sggProcessed += batch.size
                        sggIndexed += docs.size
                        sggBulkCount++

                        val globalProcessed = processedCount.addAndGet(batch.size)
                        indexedCount.addAndGet(docs.size)
                        val globalBulkCount = bulkCount.incrementAndGet()

                        val bulkTime = System.currentTimeMillis() - bulkStartTime
                        val elapsed = System.currentTimeMillis() - globalStartTime
                        val percent = String.format("%.1f", globalProcessed * 100.0 / totalCount)

                        log.info("[LC] Worker-{} 벌크 #{}/{}: {}/{} ({}%) SGG={} ({}/{}), 벌크 {}, 누적 {}",
                            workerIndex,
                            formatCount(globalBulkCount), formatCount(expectedBulks),
                            formatCount(globalProcessed), formatCount(totalCount), percent,
                            sggCode, sggIdx + 1, totalSgg,
                            formatElapsed(bulkTime), formatElapsed(elapsed))

                        // 1차 캐시 클리어 (메모리 누적 방지)
                        entityManager.clear()
                    }
            }
        }

        val sggElapsed = System.currentTimeMillis() - sggStartTime
        log.info("[LC] Worker-{} SGG {} 완료: {}건, 벌크 {}회, {}",
            workerIndex, sggCode, formatCount(sggProcessed), formatCount(sggBulkCount), formatElapsed(sggElapsed))

        return Triple(sggProcessed, sggIndexed, sggBulkCount)
    }

    private fun processBatch(entities: List<LandCharacteristic>): List<LandCompactDocument> {
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

        log.info("[LC] processBatch: summaries={}ms, outlines={}ms, trades={}ms, docs={}ms",
            t2 - t1, t3 - t2, t4 - t3, t5 - t4)

        return docs
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
    ): LandCompactDocument? {
        return try {
            val buildingPnu = PnuUtils.convertLandPnuToBuilding(entity.pnu)

            val buildingData = summariesMap[buildingPnu]?.let { toBuildingData(it) }
                ?: outlineMap[buildingPnu]?.let { toBuildingData(it) }

            val tradeData = tradeMap[entity.pnu]

            LandCompactDocument(
                pnu = entity.pnu,
                sd = PnuUtils.extractSd(entity.pnu),
                sgg = PnuUtils.extractSgg(entity.pnu),
                emd = PnuUtils.extractEmd(entity.pnu),
                land = toLandData(entity),
                building = buildingData,
                lastRealEstateTrade = tradeData
            )
        } catch (e: Exception) {
            log.warn("[LC] 문서 생성 실패 pnu={}: {}", entity.pnu, e.message)
            null
        }
    }

    private fun toLandData(entity: LandCharacteristic): LandCompactDocument.Land {
        val center = entity.center
        val geometryObj = GeoJsonUtils.toGeoJson(entity.geometry)

        return LandCompactDocument.Land(
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
            log.info("[LC] 기존 인덱스 삭제")
        }

        esClient.indices().create { c ->
            c.index(INDEX_NAME)
                .settings { s ->
                    s.numberOfShards("5")
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
                .properties("geometry") { pp -> pp.geoShape { it } }
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
            val failedItems = response.items().filter { it.error() != null }
            log.warn("[LC bulkIndex] 일부 실패: {}/{}", failedItems.size, docs.size)
            failedItems.take(3).forEach { item ->
                log.warn("[LC] 실패 id={}, reason={}", item.id(), item.error()?.reason())
            }
        }
    }
}
