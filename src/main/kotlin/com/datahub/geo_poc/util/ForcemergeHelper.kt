package com.datahub.geo_poc.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opensearch.client.opensearch.OpenSearchClient
import org.slf4j.Logger
import java.text.NumberFormat
import java.util.Locale

/**
 * Forcemerge 비동기 실행 + 세그먼트 수렴 폴링 헬퍼.
 * 모든 IndexingService에서 공용으로 사용한다.
 */
object ForcemergeHelper {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    private const val POLL_INTERVAL_MS = 5_000L
    private const val MAX_POLL_MS = 30 * 60 * 1_000L // 30분

    /**
     * 비동기로 forcemerge를 실행하고, 세그먼트 수렴까지 폴링하여 정확한 완료 시간을 로그에 기록한다.
     *
     * @param esClient OpenSearch 클라이언트
     * @param dispatcher 코루틴 디스패처
     * @param log 서비스별 로거
     * @param tag 로그 태그 (예: "LNBTPU", "LC")
     * @param indexNames forcemerge 대상 인덱스 이름 목록
     */
    fun launchAsync(
        esClient: OpenSearchClient,
        dispatcher: CoroutineDispatcher,
        log: Logger,
        tag: String,
        indexNames: List<String>
    ) {
        log.info("[{}] forcemerge 요청 (비동기, {}개 인덱스: {})", tag, indexNames.size, indexNames)

        CoroutineScope(dispatcher).launch {
            val totalStart = System.currentTimeMillis()

            coroutineScope {
                for (indexName in indexNames) {
                    launch { mergeIndex(esClient, log, tag, indexName) }
                }
            }

            val totalElapsed = System.currentTimeMillis() - totalStart
            log.info("[{}] forcemerge 전체 완료: {}", tag, formatElapsed(totalElapsed))
        }
    }

    private suspend fun mergeIndex(
        esClient: OpenSearchClient,
        log: Logger,
        tag: String,
        indexName: String
    ) {
        val startTime = System.currentTimeMillis()
        var needsPoll = false

        try {
            esClient.indices().forcemerge { f -> f.index(indexName).maxNumSegments(1L) }
            val elapsed = System.currentTimeMillis() - startTime
            log.info("[{}] forcemerge 응답 수신 [{}]: {}", tag, indexName, formatElapsed(elapsed))
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            log.info("[{}] forcemerge HTTP 타임아웃 [{}]: {} ({}), 세그먼트 수렴 대기...",
                tag, indexName, e.message, formatElapsed(elapsed))
            needsPoll = true
        }

        if (needsPoll) {
            pollUntilConverged(esClient, log, tag, indexName, startTime)
        }
    }

    private suspend fun pollUntilConverged(
        esClient: OpenSearchClient,
        log: Logger,
        tag: String,
        indexName: String,
        startTime: Long
    ) {
        val deadline = startTime + MAX_POLL_MS

        while (System.currentTimeMillis() < deadline) {
            delay(POLL_INTERVAL_MS)
            try {
                val info = getSegmentInfo(esClient, indexName)
                val elapsed = System.currentTimeMillis() - startTime

                if (info.segmentCount <= info.shardCount && info.shardCount > 0) {
                    log.info("[{}] forcemerge 완료 확인 [{}]: {} (segments={}, shards={})",
                        tag, indexName, formatElapsed(elapsed), info.segmentCount, info.shardCount)
                    return
                }

                log.info("[{}] 세그먼트 수렴 대기 중 [{}]... (segments={}/{}, {})",
                    tag, indexName, info.segmentCount, info.shardCount, formatElapsed(elapsed))
            } catch (e: Exception) {
                log.warn("[{}] 세그먼트 확인 실패 [{}]: {}", tag, indexName, e.message)
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.warn("[{}] forcemerge 폴링 타임아웃 [{}]: {} (30분 초과)", tag, indexName, formatElapsed(elapsed))
    }

    private data class SegmentInfo(val segmentCount: Int, val shardCount: Int)

    private fun getSegmentInfo(esClient: OpenSearchClient, indexName: String): SegmentInfo {
        val response = esClient.indices().segments { s -> s.index(indexName) }
        val indexSegment = response.indices()[indexName]
            ?: return SegmentInfo(0, 0)

        val shardCount = indexSegment.shards().size
        var segmentCount = 0
        for (shardList in indexSegment.shards().values) {
            for (shard in shardList) {
                segmentCount += shard.segments().size
            }
        }
        return SegmentInfo(segmentCount, shardCount)
    }

    private fun formatElapsed(ms: Long): String {
        val seconds = ms / 1000.0
        return if (seconds >= 60) {
            "${String.format("%.2f", seconds / 60)}m (${numberFormat.format(ms)}ms)"
        } else {
            "${numberFormat.format(ms)}ms (${String.format("%.2f", seconds)}s)"
        }
    }
}
