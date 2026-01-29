package com.datahub.geo_poc.util

import org.slf4j.Logger
import java.text.NumberFormat
import java.util.Locale

/**
 * 인덱싱 서비스 공통 벌크 스텝 로그 포맷터
 * 모든 IndexingService의 벌크 진행 로그를 통일된 양식으로 출력
 */
object IndexingLogHelper {
    private val nf = NumberFormat.getNumberInstance(Locale.US)

    private fun fmt(n: Number): String = nf.format(n)

    data class BulkStepLog(
        val tag: String,
        val workerIndex: Int,
        val bulkCount: Int,
        val expectedBulks: Long,
        val processed: Int,
        val totalCount: Long,
        val emdCode: String,
        val emdIdx: Int,
        val totalEmd: Int,
        // timing (ms)
        val summariesMs: Long,
        val outlinesMs: Long,
        val tradesMs: Long,
        val docsMs: Long,
        val bulkMs: Long,
        val stepTotalMs: Long,
        val accumulatedMs: Long,
        // counts (optional)
        val buildingCount: Int? = null,
        val tradeCount: Int? = null,
        val geomCount: Int? = null,
        val partitionSummary: String? = null,
    )

    fun logBulkStep(log: Logger, s: BulkStepLog) {
        val pct = String.format("%.1f", s.processed * 100.0 / s.totalCount)

        val sb = StringBuilder()
        sb.append("[${s.tag}] W-${s.workerIndex}")
        sb.append(" 벌크 #${fmt(s.bulkCount)}/${fmt(s.expectedBulks)}:")
        sb.append(" ${fmt(s.processed)}/${fmt(s.totalCount)} ($pct%)")
        sb.append(" EMD=${s.emdCode} (${s.emdIdx + 1}/${s.totalEmd})")

        if (s.partitionSummary != null) {
            sb.append(" → ${s.partitionSummary}")
        }

        val counts = mutableListOf<String>()
        if (s.buildingCount != null) counts.add("건물 ${fmt(s.buildingCount)}")
        if (s.tradeCount != null) counts.add("실거래 ${fmt(s.tradeCount)}")
        if (s.geomCount != null) counts.add("geom ${fmt(s.geomCount)}")
        if (counts.isNotEmpty()) {
            sb.append(" | ${counts.joinToString(" ")}")
        }

        sb.append(" | sum=${s.summariesMs}ms out=${s.outlinesMs}ms trd=${s.tradesMs}ms doc=${s.docsMs}ms blk=${s.bulkMs}ms")

        val stepSec = String.format("%.2f", s.stepTotalMs / 1000.0)
        sb.append(" | 스텝 ${fmt(s.stepTotalMs)}ms (${stepSec}s)")

        val accSec = s.accumulatedMs / 1000.0
        val accStr = if (accSec >= 60) {
            "${String.format("%.2f", accSec / 60)}m (${String.format("%.1f", accSec)}s)"
        } else {
            "${String.format("%.2f", accSec)}s"
        }
        sb.append(" 누적 $accStr")

        log.info("{}", sb.toString())
    }
}
