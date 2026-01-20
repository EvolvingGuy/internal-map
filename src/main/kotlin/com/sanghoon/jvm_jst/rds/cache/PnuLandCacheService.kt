package com.sanghoon.jvm_jst.rds.cache

import com.google.protobuf.ByteString
import com.sanghoon.jvm_jst.rds.proto.PnuLandDataProto
import com.sanghoon.jvm_jst.rds.service.PnuLandData
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * PNU 토지 데이터 캐시 서비스
 */
@Service
class PnuLandCacheService(
    private val pnuLandDataRedisTemplate: RedisTemplate<String, PnuLandDataProto>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val TTL: Duration = Duration.ofDays(1)
        const val PREFIX = "pnu_land:"
    }

    /**
     * PNU 목록으로 캐시 조회
     * @return Map<pnu, PnuLandData> - 캐시 히트된 것만 반환
     */
    fun multiGet(pnus: Collection<String>): Map<String, PnuLandData> {
        if (pnus.isEmpty()) return emptyMap()

        val keys = pnus.map { "$PREFIX$it" }
        val values = pnuLandDataRedisTemplate.opsForValue().multiGet(keys) ?: return emptyMap()

        val result = mutableMapOf<String, PnuLandData>()
        pnus.forEachIndexed { idx, pnu ->
            val proto = values.getOrNull(idx)
            if (proto != null) {
                try {
                    result[pnu] = proto.toDto()
                } catch (e: Exception) {
                    log.warn("[PnuLandCache] Failed to parse cache for pnu={}: {}", pnu, e.message)
                }
            }
        }
        return result
    }

    /**
     * PNU 토지 데이터 캐시 저장
     */
    fun multiSet(data: Map<String, PnuLandData>) {
        if (data.isEmpty()) return

        val ops = pnuLandDataRedisTemplate.opsForValue()
        for ((pnu, landData) in data) {
            val key = "$PREFIX$pnu"
            val proto = landData.toProto()
            ops.set(key, proto, TTL)
        }
    }

    // DTO <-> Proto 변환
    private fun PnuLandDataProto.toDto(): PnuLandData =
        PnuLandData.fromWkb(
            pnu = pnu,
            geometryWkb = geometryWkb.toByteArray(),
            centerWkb = centerWkb.toByteArray(),
            area = if (hasArea()) area else null,
            isDonut = if (hasIsDonut()) isDonut else null
        )

    private fun PnuLandData.toProto(): PnuLandDataProto {
        val builder = PnuLandDataProto.newBuilder()
            .setPnu(pnu)
            .setGeometryWkb(ByteString.copyFrom(geometryToWkb()))
            .setCenterWkb(ByteString.copyFrom(centerToWkb()))

        area?.let { builder.setArea(it) }
        isDonut?.let { builder.setIsDonut(it) }

        return builder.build()
    }
}
