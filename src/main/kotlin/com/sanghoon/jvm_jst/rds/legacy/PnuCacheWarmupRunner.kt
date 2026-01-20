package com.sanghoon.jvm_jst.rds.legacy

import com.sanghoon.jvm_jst.legacy.BoundaryRegionCache
import com.sanghoon.jvm_jst.legacy.BoundaryRegionCacheReadyEvent
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class PnuCacheWarmupRunner(
    private val pnuCacheService: PnuCacheService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dispatcher = Dispatchers.IO.limitedParallelism(5)

    companion object {
        val MAJOR_CITY_CODES = setOf("11", "26", "27", "28", "29", "30", "36")
    }

    @EventListener
    fun onBoundaryRegionCacheReady(@Suppress("UNUSED_PARAMETER") event: BoundaryRegionCacheReadyEvent) {
        warmupBySido(MAJOR_CITY_CODES, "Startup")
    }

    /** 특정 시도만 워밍업 */
    fun warmupBySido(sidoCodes: Set<String>, label: String = "Manual"): WarmupResult {
        val dongCodes = BoundaryRegionCache.getDongCodesBySido(sidoCodes)
        return executeWarmup(dongCodes, label)
    }

    /** 전체 워밍업 */
    fun warmupAll(): WarmupResult {
        val dongCodes = BoundaryRegionCache.getAllDongCodes()
        return executeWarmup(dongCodes, "Full")
    }

    private fun executeWarmup(dongCodes: List<String>, label: String): WarmupResult {
        if (dongCodes.isEmpty()) {
            log.warn("[PNU Warmup][$label] No dong codes")
            return WarmupResult(0, 0, 0)
        }

        log.info("[PNU Warmup][$label] START - ${dongCodes.size} codes")
        val startTime = System.currentTimeMillis()

        val batches = dongCodes.chunked(100)
        val warmedUp = AtomicInteger(0)
        val failed = AtomicInteger(0)

        runBlocking(dispatcher) {
            batches.map { batch ->
                async {
                    try {
                        pnuCacheService.getByRegionCodes(batch)
                        warmedUp.addAndGet(batch.size)
                    } catch (e: Exception) {
                        failed.addAndGet(batch.size)
                        log.error("[PNU Warmup][$label] FAILED: ${e.message}")
                    }
                }
            }.awaitAll()
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[PNU Warmup][$label] DONE - cached=${warmedUp.get()}, failed=${failed.get()}, time=${elapsed}ms")
        return WarmupResult(warmedUp.get(), failed.get(), elapsed)
    }
}

data class WarmupResult(
    val cached: Int,
    val failed: Int,
    val elapsedMs: Long
)
