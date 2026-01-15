package com.sanghoon.jvm_jst.search

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

private val logger = LoggerFactory.getLogger(SearchController::class.java)

data class SearchResult(
    val pnu: String,
    val bjdongCd: String,
    val jibunAddress: String?,
    val roadAddress: String?,
    val buildingName: String?,
    val lat: Double?,
    val lng: Double?
)

data class SearchResponse(
    val results: List<SearchResult>,
    val count: Int,
    val elapsedMs: Long
)

@Controller
class SearchController(
    private val searchRepository: SearchRepository
) {

    @GetMapping("/search")
    fun searchPage(): String {
        return "search"
    }

    @GetMapping("/api/search")
    @ResponseBody
    fun search(@RequestParam keyword: String): SearchResponse {
        if (keyword.isBlank()) return SearchResponse(emptyList(), 0, 0)

        val start = System.currentTimeMillis()
        val results = searchRepository.searchByKeyword(keyword).map { row ->
            SearchResult(
                pnu = row[0] as String,
                bjdongCd = row[1] as String,
                jibunAddress = row[2] as? String,
                roadAddress = row[3] as? String,
                buildingName = row[4] as? String,
                lat = row[5] as? Double,
                lng = row[6] as? Double
            )
        }
        val elapsed = System.currentTimeMillis() - start
        logger.info("[Search] keyword='{}', count={}, elapsed={}ms", keyword, results.size, elapsed)
        return SearchResponse(results, results.size, elapsed)
    }
}
