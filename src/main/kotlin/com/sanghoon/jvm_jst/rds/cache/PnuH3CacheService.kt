package com.sanghoon.jvm_jst.rds.cache

import com.sanghoon.jvm_jst.rds.proto.PnuSetProto
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * H3 인덱스별 PNU Set 캐시 서비스
 */
@Service
class PnuH3CacheService(
    private val pnuSetRedisTemplate: RedisTemplate<String, PnuSetProto>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val TTL: Duration = Duration.ofDays(1)
        const val PREFIX = "pnu_h3_10:"
    }

    /**
     * 여러 H3 인덱스의 PNU Set 조회
     * @return Map<h3Index, Set<pnu>> - 캐시 히트된 것만 반환
     */
    fun multiGet(h3Indexes: Collection<Long>): Map<Long, Set<String>> {
        if (h3Indexes.isEmpty()) return emptyMap()

        val keys = h3Indexes.map { "$PREFIX$it" }
        val values = pnuSetRedisTemplate.opsForValue().multiGet(keys) ?: return emptyMap()

        val result = mutableMapOf<Long, Set<String>>()
        h3Indexes.forEachIndexed { idx, h3Index ->
            val proto = values.getOrNull(idx)
            if (proto != null) {
                // 빈 Set도 캐시 히트로 처리 (negative caching)
                result[h3Index] = proto.pnusList.toSet()
            }
        }
        return result
    }

    /**
     * 여러 H3 인덱스의 PNU Set 캐시 저장
     */
    fun multiSet(data: Map<Long, Set<String>>) {
        if (data.isEmpty()) return

        val ops = pnuSetRedisTemplate.opsForValue()
        for ((h3Index, pnuSet) in data) {
            val key = "$PREFIX$h3Index"
            val proto = PnuSetProto.newBuilder()
                .addAllPnus(pnuSet)
                .build()
            ops.set(key, proto, TTL)
        }
    }
}
