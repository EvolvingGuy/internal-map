package com.sanghoon.jvm_jst.legacy.h3

import com.sanghoon.jvm_jst.legacy.h3.proto.H3Emd10Proto.BjdongCellDataProto
import com.sanghoon.jvm_jst.legacy.h3.proto.H3Emd10Proto.BjdongCellListProto
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

// @Component  // legacy - disabled
class H3RedisEmd10Preloader(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
    private val repository: H3AggEmdRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CACHE_PREFIX = "h3:emd10:proto:"
        private const val CHECK_KEY = "h3:emd10:proto:check"

        private val ALL_SIDO_CODES = listOf(
            "11", "26", "27", "28", "29", "30", "31", "36",
            "41", "43", "44", "46", "47", "48", "50", "51", "52"
        )
    }

    @PostConstruct
    fun init() {
        Thread {
            try {
                preload()
            } catch (e: Exception) {
                log.error("H3RedisEmd10 프리로드 실패", e)
            }
        }.start()
    }

    private fun preload() {
        val startTime = System.currentTimeMillis()

        // Redis에 이미 캐시되어 있는지 확인
        val checkStart = System.currentTimeMillis()
        val existingKey = redisTemplate.opsForValue().get(CHECK_KEY)
        val checkTime = System.currentTimeMillis() - checkStart

        if (existingKey != null) {
            log.info("H3RedisEmd10 프리로드 스킵 - Redis 캐시 존재 확인 {}ms", checkTime)
            return
        }

        log.info("H3RedisEmd10 프리로드 시작 (시도 {}개)", ALL_SIDO_CODES.size)
        var totalH3Cells = 0
        var totalRecords = 0L
        var totalBytes = 0L

        for (sidoCode in ALL_SIDO_CODES) {
            val stepStart = System.currentTimeMillis()

            // DB 조회
            val dbStart = System.currentTimeMillis()
            val minCode = sidoCode.toInt() * 1000000
            val maxCode = (sidoCode.toInt() + 1) * 1000000
            val rows = repository.findBySidoCodeRange(minCode, maxCode)
            val dbTime = System.currentTimeMillis() - dbStart

            // H3 인덱스별 그룹핑
            val groupStart = System.currentTimeMillis()
            val grouped = rows.groupBy { it[1] as String }
            val groupTime = System.currentTimeMillis() - groupStart

            // Redis 저장
            val redisStart = System.currentTimeMillis()
            var h3CellCount = 0
            var recordCount = 0
            var byteCount = 0L

            for ((h3Index, group) in grouped) {
                val protoBuilder = BjdongCellListProto.newBuilder()
                for (row in group) {
                    protoBuilder.addCells(
                        BjdongCellDataProto.newBuilder()
                            .setBjdongCd((row[0] as Number).toInt())
                            .setCnt((row[2] as Number).toInt())
                            .setSumLat((row[3] as Number).toDouble())
                            .setSumLng((row[4] as Number).toDouble())
                            .build()
                    )
                }
                val protoBytes = protoBuilder.build().toByteArray()
                redisTemplate.opsForValue().set("$CACHE_PREFIX$h3Index", protoBytes)

                h3CellCount++
                recordCount += group.size
                byteCount += protoBytes.size
            }
            val redisTime = System.currentTimeMillis() - redisStart

            totalH3Cells += h3CellCount
            totalRecords += recordCount
            totalBytes += byteCount

            val stepTime = System.currentTimeMillis() - stepStart
            val stepMB = byteCount / 1024.0 / 1024.0
            val totalMB = totalBytes / 1024.0 / 1024.0

            log.info(
                "프리로드 [{}] H3셀={}개, 레코드={}개, +{} MB (누적 {} MB) | db={}ms, group={}ms, redis={}ms, total={}ms",
                sidoCode, h3CellCount, recordCount,
                String.format("%.2f", stepMB), String.format("%.2f", totalMB),
                dbTime, groupTime, redisTime, stepTime
            )
        }

        // 캐시 완료 마커 저장
        redisTemplate.opsForValue().set(CHECK_KEY, "done".toByteArray())

        val totalTime = System.currentTimeMillis() - startTime
        val finalMB = totalBytes / 1024.0 / 1024.0
        log.info(
            "H3RedisEmd10 프리로드 완료 - H3셀={}개, 레코드={}개, {} MB, 총 {}ms",
            totalH3Cells, totalRecords, String.format("%.2f", finalMB), totalTime
        )
    }
}
