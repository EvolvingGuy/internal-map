package com.sanghoon.jvm_jst.h3

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/h3cell")
class H3CellController(
    private val h3CellCacheService: H3CellCacheService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 읍면동 레벨 (res 10, ~66m) - 격자 단위 캐시
     */
    @GetMapping("/emd")
    fun getEmdCells(request: H3AggRequest): H3CellResponse {
        val startTime = System.currentTimeMillis()
        val cells = h3CellCacheService.getEmdCellsByBbox(request.toBBox())
        val elapsed = System.currentTimeMillis() - startTime
        return toResponse(cells, elapsed)
    }

    /**
     * 시군구 레벨 (res 8, ~460m) - 격자 단위 캐시
     */
    @GetMapping("/sgg")
    fun getSggCells(request: H3AggRequest): H3CellResponse {
        val startTime = System.currentTimeMillis()
        val cells = h3CellCacheService.getSggCellsByBbox(request.toBBox())
        val elapsed = System.currentTimeMillis() - startTime
        return toResponse(cells, elapsed)
    }

    /**
     * 시도 레벨 (res 5, ~8km) - 격자 단위 캐시
     */
    @GetMapping("/sd")
    fun getSdCells(request: H3AggRequest): H3CellResponse {
        val startTime = System.currentTimeMillis()
        val cells = h3CellCacheService.getSdCellsByBbox(request.toBBox())
        val elapsed = System.currentTimeMillis() - startTime
        return toResponse(cells, elapsed)
    }

    private fun toResponse(cells: List<H3CellData>, elapsed: Long): H3CellResponse {
        val dtos = cells.map { cell ->
            H3CellDto(
                h3Index = cell.h3Index,
                cnt = cell.cnt,
                lat = cell.avgLat(),
                lng = cell.avgLng(),
                regionCode = ""
            )
        }
        return H3CellResponse(dtos, dtos.sumOf { it.cnt }, elapsed)
    }
}

data class H3CellResponse(
    val cells: List<H3CellDto>,
    val totalCount: Int,
    val elapsedMs: Long
)
