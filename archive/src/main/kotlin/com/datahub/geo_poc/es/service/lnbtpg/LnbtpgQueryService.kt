package com.datahub.geo_poc.es.service.lnbtpg

import org.opensearch.client.opensearch.OpenSearchClient
import com.datahub.geo_poc.es.document.land.LnbtpgDocument
import com.datahub.geo_poc.util.GeometryCompressor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * LNBTPG Query 서비스 - Marker API 전용
 * bbox 내 필지 조회 + geometry 디코딩
 */
@Service
class LnbtpgQueryService(
    private val esClient: OpenSearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INDEX_NAME = LnbtpgDocument.INDEX_NAME
        const val DEFAULT_LIMIT = 500
        const val MAX_LIMIT = 2000
    }

    /**
     * bbox 내 마커 조회 (geometry 포함)
     */
    fun findMarkers(
        swLng: Double,
        swLat: Double,
        neLng: Double,
        neLat: Double,
        limit: Int = DEFAULT_LIMIT
    ): MarkerResponse {
        val startTime = System.currentTimeMillis()
        val effectiveLimit = limit.coerceIn(1, MAX_LIMIT)

        val response = esClient.search({ s ->
            s.index(INDEX_NAME)
                .size(effectiveLimit)
                .query { q ->
                    q.geoBoundingBox { geo ->
                        geo.field("land.center")
                            .boundingBox { bb ->
                                bb.tlbr { tlbr ->
                                    tlbr.topLeft { tl -> tl.latlon { ll -> ll.lat(neLat).lon(swLng) } }
                                        .bottomRight { br -> br.latlon { ll -> ll.lat(swLat).lon(neLng) } }
                                }
                            }
                    }
                }
        }, Map::class.java)

        val markers = response.hits().hits().mapNotNull { hit ->
            parseMarker(hit.source())
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[LNBTPG Marker] bbox=[{},{},{},{}], limit={}, results={}, elapsed={}ms",
            swLng, swLat, neLng, neLat, effectiveLimit, markers.size, elapsed)

        return MarkerResponse(
            count = markers.size,
            markers = markers,
            elapsedMs = elapsed
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMarker(source: Map<*, *>?): Marker? {
        if (source == null) return null

        val pnu = source["pnu"]?.toString() ?: return null
        val land = source["land"] as? Map<String, Any>
        val geometryBinary = source["geometryBinary"]?.toString()
        val buildingsRaw = source["buildings"] as? List<Map<String, Any>> ?: emptyList()
        val tradesRaw = source["trades"] as? List<Map<String, Any>> ?: emptyList()

        val center = land?.get("center") as? Map<String, Any>
        val centerLat = (center?.get("lat") as? Number)?.toDouble() ?: return null
        val centerLon = (center?.get("lon") as? Number)?.toDouble() ?: return null

        // geometry 디코딩: Base64 -> gzip 해제 -> WKB -> GeoJSON
        val geometry = GeometryCompressor.decompressToGeoJson(geometryBinary)

        return Marker(
            pnu = pnu,
            center = MarkerCenter(centerLat, centerLon),
            geometry = geometry,
            land = land?.let {
                MarkerLand(
                    jiyukCd1 = it["jiyukCd1"]?.toString(),
                    jimokCd = it["jimokCd"]?.toString(),
                    area = (it["area"] as? Number)?.toDouble(),
                    price = (it["price"] as? Number)?.toLong()
                )
            },
            buildings = buildingsRaw.map { b ->
                MarkerBuilding(
                    mgmBldrgstPk = b["mgmBldrgstPk"]?.toString(),
                    mainPurpsCdNm = b["mainPurpsCdNm"]?.toString(),
                    regstrGbCdNm = b["regstrGbCdNm"]?.toString(),
                    useAprDay = parseLocalDate(b["useAprDay"]?.toString()),
                    totArea = (b["totArea"] as? Number)?.let { BigDecimal.valueOf(it.toDouble()) }
                )
            },
            trades = tradesRaw.map { t ->
                MarkerTrade(
                    property = t["property"]?.toString(),
                    contractDate = parseLocalDate(t["contractDate"]?.toString()),
                    effectiveAmount = (t["effectiveAmount"] as? Number)?.toLong(),
                    buildingAmountPerM2 = (t["buildingAmountPerM2"] as? Number)?.let { BigDecimal.valueOf(it.toDouble()) },
                    landAmountPerM2 = (t["landAmountPerM2"] as? Number)?.let { BigDecimal.valueOf(it.toDouble()) }
                )
            }
        )
    }

    private fun parseLocalDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value.take(10))
        } catch (e: Exception) {
            null
        }
    }

    // Response Models
    data class MarkerResponse(
        val count: Int,
        val markers: List<Marker>,
        val elapsedMs: Long
    )

    data class Marker(
        val pnu: String,
        val center: MarkerCenter,
        val geometry: Map<String, Any>?,
        val land: MarkerLand?,
        val buildings: List<MarkerBuilding>,
        val trades: List<MarkerTrade>
    )

    data class MarkerCenter(
        val lat: Double,
        val lon: Double
    )

    data class MarkerLand(
        val jiyukCd1: String?,
        val jimokCd: String?,
        val area: Double?,
        val price: Long?
    )

    data class MarkerBuilding(
        val mgmBldrgstPk: String?,
        val mainPurpsCdNm: String?,
        val regstrGbCdNm: String?,
        val useAprDay: LocalDate?,
        val totArea: BigDecimal?
    )

    data class MarkerTrade(
        val property: String?,
        val contractDate: LocalDate?,
        val effectiveAmount: Long?,
        val buildingAmountPerM2: BigDecimal?,
        val landAmountPerM2: BigDecimal?
    )
}
