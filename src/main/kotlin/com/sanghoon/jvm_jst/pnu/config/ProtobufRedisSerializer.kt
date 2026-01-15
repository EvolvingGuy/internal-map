package com.sanghoon.jvm_jst.pnu.config

import com.google.protobuf.Message
import com.google.protobuf.Parser
import org.springframework.data.redis.serializer.RedisSerializer

class ProtobufRedisSerializer<T : Message>(
    private val defaultInstance: T
) : RedisSerializer<T> {

    @Suppress("UNCHECKED_CAST")
    private val parser: Parser<T> = defaultInstance.parserForType as Parser<T>

    override fun serialize(t: T?): ByteArray {
        return t?.toByteArray() ?: ByteArray(0)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(bytes: ByteArray?): T? {
        if (bytes == null) return null
        // 빈 byte array = 빈 proto (negative caching 지원)
        if (bytes.isEmpty()) return defaultInstance as T
        return parser.parseFrom(bytes)
    }
}
