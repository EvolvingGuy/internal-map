package com.datahub.geo_poc.es.lbt17x4regionnofm

import com.datahub.geo_poc.util.ForcemergeHelper
import kotlinx.coroutines.CoroutineDispatcher
import org.opensearch.client.opensearch.OpenSearchClient
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/es/lbt-17x4-region-nofm")
class Lbt17x4regionNofmRestController(
    private val indexingService: Lbt17x4regionNofmIndexingService,
    private val esClient: OpenSearchClient,
    private val indexingDispatcher: CoroutineDispatcher
) {
    private val log = LoggerFactory.getLogger(javaClass)
    @PutMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.reindex())
    }

    @PutMapping("/reindex/no-refresh")
    fun reindexNoRefresh(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.reindex(disableRefresh = true))
    }

    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        return ResponseEntity.ok(indexingService.count())
    }

    @PutMapping("/forcemerge/{sdCode}")
    fun forcemerge(@PathVariable sdCode: String): ResponseEntity<Map<String, Any>> {
        val indexName = Lbt17x4regionNofmDocument.indexName(sdCode)
        if (sdCode !in Lbt17x4regionNofmDocument.SD_CODES) {
            return ResponseEntity.badRequest().body(mapOf("error" to "invalid sdCode: $sdCode", "valid" to Lbt17x4regionNofmDocument.SD_CODES.joinToString()))
        }
        ForcemergeHelper.launchAsync(esClient, indexingDispatcher, log, "LBT_17x4_REGION_NOFM", listOf(indexName))
        return ResponseEntity.ok(mapOf(
            "action" to "forcemerge",
            "status" to "started",
            "async" to true,
            "index" to indexName
        ))
    }

    @GetMapping("/segments")
    fun segments(): ResponseEntity<Map<String, Any>> {
        val result = mutableMapOf<String, Any>()
        var totalSegments = 0
        var totalShards = 0
        for (indexName in Lbt17x4regionNofmDocument.allIndexNames()) {
            try {
                val response = esClient.indices().segments { s -> s.index(indexName) }
                val indexSegment = response.indices()[indexName]
                if (indexSegment != null) {
                    val shardCount = indexSegment.shards().size
                    var segmentCount = 0
                    for (shardList in indexSegment.shards().values) {
                        for (shard in shardList) {
                            segmentCount += shard.segments().size
                        }
                    }
                    result[indexName] = mapOf("shards" to shardCount, "segments" to segmentCount)
                    totalSegments += segmentCount
                    totalShards += shardCount
                }
            } catch (e: Exception) {
                result[indexName] = mapOf("error" to (e.message ?: "unknown"))
            }
        }
        result["total"] = mapOf("shards" to totalShards, "segments" to totalSegments)
        return ResponseEntity.ok(result)
    }

    @DeleteMapping
    fun delete(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(indexingService.deleteIndex())
    }
}
