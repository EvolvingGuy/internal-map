package com.sanghoon.jvm_jst.pnu.cache

import com.sanghoon.jvm_jst.pnu.proto.*
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * H3 인덱스별 Agg 데이터 캐시 DTO
 */
data class AggCacheData(
    val code: Long,
    val cnt: Int,
    val sumLat: Double,
    val sumLng: Double
)

/**
 * 고정형 리전 캐시 DTO
 */
data class StaticRegionCacheData(
    val code: Long,
    val name: String,
    val cnt: Int,
    val centerLat: Double,
    val centerLng: Double
)

/**
 * Redis 캐시 서비스 (Protobuf 직렬화)
 */
@Service
class PnuAggCacheService(
    private val aggCacheRedisTemplate: RedisTemplate<String, AggCacheListProto>,
    private val staticRegionCacheRedisTemplate: RedisTemplate<String, StaticRegionCacheListProto>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val TTL: Duration = Duration.ofDays(1)

        const val PREFIX_EMD_11 = "pnu_agg_emd_11:"
        const val PREFIX_EMD_10 = "pnu_agg_emd_10:"
        const val PREFIX_EMD_09 = "pnu_agg_emd_09:"
        const val PREFIX_SGG_08 = "pnu_agg_sgg_08:"
        const val PREFIX_SGG_07 = "pnu_agg_sgg_07:"
        const val PREFIX_SD_06 = "pnu_agg_sd_06:"
        const val PREFIX_SD_05 = "pnu_agg_sd_05:"
        const val PREFIX_STATIC = "pnu_agg_static:"
    }

    fun multiGet(prefix: String, h3Indexes: Collection<Long>): Map<Long, List<AggCacheData>> {
        if (h3Indexes.isEmpty()) return emptyMap()

        val keys = h3Indexes.map { "$prefix$it" }
        val values = aggCacheRedisTemplate.opsForValue().multiGet(keys) ?: return emptyMap()

        val result = mutableMapOf<Long, List<AggCacheData>>()
        h3Indexes.forEachIndexed { idx, h3Index ->
            val proto = values.getOrNull(idx)
            if (proto != null && proto.itemsCount > 0) {
                result[h3Index] = proto.itemsList.map { it.toDto() }
            }
        }
        return result
    }

    fun multiSet(prefix: String, data: Map<Long, List<AggCacheData>>) {
        if (data.isEmpty()) return

        val ops = aggCacheRedisTemplate.opsForValue()
        for ((h3Index, list) in data) {
            val key = "$prefix$h3Index"
            val proto = list.toProto()
            ops.set(key, proto, TTL)
        }
    }

    fun multiGetStatic(level: String, h3Indexes: Collection<Long>): Map<Long, List<StaticRegionCacheData>> {
        if (h3Indexes.isEmpty()) return emptyMap()

        val keys = h3Indexes.map { "$PREFIX_STATIC$level:$it" }
        val values = staticRegionCacheRedisTemplate.opsForValue().multiGet(keys) ?: return emptyMap()

        val result = mutableMapOf<Long, List<StaticRegionCacheData>>()
        h3Indexes.forEachIndexed { idx, h3Index ->
            val proto = values.getOrNull(idx)
            if (proto != null && proto.itemsCount > 0) {
                result[h3Index] = proto.itemsList.map { it.toDto() }
            }
        }
        return result
    }

    fun multiSetStatic(level: String, data: Map<Long, List<StaticRegionCacheData>>) {
        if (data.isEmpty()) return

        val ops = staticRegionCacheRedisTemplate.opsForValue()
        for ((h3Index, list) in data) {
            val key = "$PREFIX_STATIC$level:$h3Index"
            val proto = list.toStaticProto()
            ops.set(key, proto, TTL)
        }
    }

    // DTO <-> Proto 변환 확장함수
    private fun AggCacheDataProto.toDto() = AggCacheData(code, cnt, sumLat, sumLng)

    private fun List<AggCacheData>.toProto(): AggCacheListProto =
        AggCacheListProto.newBuilder()
            .addAllItems(map {
                AggCacheDataProto.newBuilder()
                    .setCode(it.code)
                    .setCnt(it.cnt)
                    .setSumLat(it.sumLat)
                    .setSumLng(it.sumLng)
                    .build()
            })
            .build()

    private fun StaticRegionCacheDataProto.toDto() =
        StaticRegionCacheData(code, name, cnt, centerLat, centerLng)

    private fun List<StaticRegionCacheData>.toStaticProto(): StaticRegionCacheListProto =
        StaticRegionCacheListProto.newBuilder()
            .addAllItems(map {
                StaticRegionCacheDataProto.newBuilder()
                    .setCode(it.code)
                    .setName(it.name)
                    .setCnt(it.cnt)
                    .setCenterLat(it.centerLat)
                    .setCenterLng(it.centerLng)
                    .build()
            })
            .build()
}
