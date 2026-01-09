package com.sanghoon.jvm_jst.h3

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class H3Emd9Cache(
    private val repository: H3AggEmd9Repository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // h3Index -> List<BjdongCellData>
    private val cache = ConcurrentHashMap<String, List<BjdongCellData>>()

    // 로드된 h3Index 집합 (빈 결과도 로드된 것으로 처리)
    private val loadedKeys = ConcurrentHashMap.newKeySet<String>()

    companion object {
        // BjdongCellData 추정 크기: bjdongCd(~40B) + cnt(4B) + sumLat(8B) + sumLng(8B) + 객체헤더(~16B) ≈ 80B
        private const val BYTES_PER_RECORD = 80L

        // 전체 시도 코드
        private val ALL_SIDO_CODES = listOf(
            "11", // 서울
            "26", // 부산
            "27", // 대구
            "28", // 인천
            "29", // 광주
            "30", // 대전
            "31", // 울산
            "36", // 세종
            "41", // 경기
            "43", // 충북
            "44", // 충남
            "46", // 전남
            "47", // 경북
            "48", // 경남
            "50", // 제주
            "51", // 강원
            "52"  // 전북
        )
    }

    @PostConstruct
    fun init() {
        Thread {
            log.info("H3Emd9Cache 프리로드 시작 (시도 {}개)", ALL_SIDO_CODES.size)
            val totalStart = System.currentTimeMillis()
            var totalH3Cells = 0
            var totalRecords = 0L

            for (sidoCode in ALL_SIDO_CODES) {
                try {
                    val stepStart = System.currentTimeMillis()
                    val rows = repository.findBySidoCode(sidoCode)

                    var h3CellCount = 0
                    var recordCount = 0

                    rows.groupBy { it[1] as String }
                        .forEach { (h3Index, group) ->
                            val list = group.map { row ->
                                BjdongCellData(
                                    bjdongCd = row[0] as String,
                                    cnt = (row[2] as Number).toInt(),
                                    sumLat = (row[3] as Number).toDouble(),
                                    sumLng = (row[4] as Number).toDouble()
                                )
                            }
                            cache[h3Index] = list
                            loadedKeys.add(h3Index)
                            h3CellCount++
                            recordCount += list.size
                        }

                    totalH3Cells += h3CellCount
                    totalRecords += recordCount
                    val stepElapsed = System.currentTimeMillis() - stepStart
                    val stepMB = (recordCount * BYTES_PER_RECORD) / 1024.0 / 1024.0
                    val totalMB = (totalRecords * BYTES_PER_RECORD) / 1024.0 / 1024.0

                    log.info("프리로드 [{}] H3셀={}개, 레코드={}개, +{}MB (누적 {}MB), {}ms",
                        sidoCode, h3CellCount, recordCount,
                        String.format("%.2f", stepMB), String.format("%.2f", totalMB), stepElapsed)
                } catch (e: Exception) {
                    log.error("프리로드 실패 - 시도: {}", sidoCode, e)
                }
            }

            totalRecordCount = totalRecords
            val totalElapsed = System.currentTimeMillis() - totalStart
            val finalMB = (totalRecords * BYTES_PER_RECORD) / 1024.0 / 1024.0
            log.info("H3Emd9Cache 프리로드 완료: H3셀={}개, 레코드={}개, {}MB, 총 {}ms",
                totalH3Cells, totalRecords, String.format("%.2f", finalMB), totalElapsed)
        }.start()
    }

    @Volatile
    private var totalRecordCount = 0L

    fun get(h3Indexes: Collection<String>): Map<String, List<BjdongCellData>> {
        val result = HashMap<String, List<BjdongCellData>>(h3Indexes.size)
        val toLoad = mutableListOf<String>()

        for (h3Index in h3Indexes) {
            val cached = cache[h3Index]
            if (cached != null) {
                result[h3Index] = cached
            } else if (!loadedKeys.contains(h3Index)) {
                toLoad.add(h3Index)
            }
        }

        if (toLoad.isNotEmpty()) {
            loadFromDb(toLoad, result)
        }

        return result
    }

    private fun loadFromDb(h3Indexes: List<String>, result: MutableMap<String, List<BjdongCellData>>) {
        val rows = repository.findByH3IndexesWithBjdong(h3Indexes)
        val grouped = rows.groupBy { it[1] as String }

        for (h3Index in h3Indexes) {
            val group = grouped[h3Index]
            if (group != null) {
                val list = group.map { row ->
                    BjdongCellData(
                        bjdongCd = row[0] as String,
                        cnt = (row[2] as Number).toInt(),
                        sumLat = (row[3] as Number).toDouble(),
                        sumLng = (row[4] as Number).toDouble()
                    )
                }
                cache[h3Index] = list
                result[h3Index] = list
                totalRecordCount += list.size
            }
            loadedKeys.add(h3Index)
        }
    }

    fun getCacheSize(): Int = cache.size

    fun getCacheSizeMB(): String {
        val bytes = totalRecordCount * BYTES_PER_RECORD
        return String.format("%.2f", bytes / 1024.0 / 1024.0)
    }

    fun getLoadedKeysSize(): Int = loadedKeys.size
}
