package com.sanghoon.jvm_jst.legacy.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer

// @Configuration  // legacy - disabled
class RedisConfig {

    @Bean
    fun byteArrayRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, ByteArray> {
        return RedisTemplate<String, ByteArray>().apply {
            this.connectionFactory = connectionFactory
            keySerializer = RedisSerializer.string()
            valueSerializer = RedisSerializer.byteArray()
            hashKeySerializer = RedisSerializer.string()
            hashValueSerializer = RedisSerializer.byteArray()
        }
    }
}
