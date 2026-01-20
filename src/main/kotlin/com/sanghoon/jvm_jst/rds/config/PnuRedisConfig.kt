package com.sanghoon.jvm_jst.rds.config

import com.sanghoon.jvm_jst.rds.proto.AggCacheListProto
import com.sanghoon.jvm_jst.rds.proto.PnuLandDataProto
import com.sanghoon.jvm_jst.rds.proto.PnuSetProto
import com.sanghoon.jvm_jst.rds.proto.StaticRegionCacheListProto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class PnuRedisConfig {

    @Bean
    fun byteArrayRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, ByteArray> {
        return RedisTemplate<String, ByteArray>().apply {
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = RedisSerializer.byteArray()
        }
    }

    @Bean
    fun aggCacheRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, AggCacheListProto> {
        return RedisTemplate<String, AggCacheListProto>().apply {
            connectionFactory = factory
            keySerializer = StringRedisSerializer()
            valueSerializer = ProtobufRedisSerializer(AggCacheListProto.getDefaultInstance())
        }
    }

    @Bean
    fun staticRegionCacheRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, StaticRegionCacheListProto> {
        return RedisTemplate<String, StaticRegionCacheListProto>().apply {
            connectionFactory = factory
            keySerializer = StringRedisSerializer()
            valueSerializer = ProtobufRedisSerializer(StaticRegionCacheListProto.getDefaultInstance())
        }
    }

    @Bean
    fun pnuSetRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, PnuSetProto> {
        return RedisTemplate<String, PnuSetProto>().apply {
            connectionFactory = factory
            keySerializer = StringRedisSerializer()
            valueSerializer = ProtobufRedisSerializer(PnuSetProto.getDefaultInstance())
        }
    }

    @Bean
    fun pnuLandDataRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, PnuLandDataProto> {
        return RedisTemplate<String, PnuLandDataProto>().apply {
            connectionFactory = factory
            keySerializer = StringRedisSerializer()
            valueSerializer = ProtobufRedisSerializer(PnuLandDataProto.getDefaultInstance())
        }
    }
}
