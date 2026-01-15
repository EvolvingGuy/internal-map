package com.sanghoon.jvm_jst.pnu.config

import com.google.protobuf.Message
import com.google.protobuf.Parser
import org.springframework.data.redis.serializer.RedisSerializer

class ProtobufRedisSerializer<T : Message>(
    defaultInstance: T
) : RedisSerializer<T> {

    @Suppress("UNCHECKED_CAST")
    private val parser: Parser<T> = defaultInstance.parserForType as Parser<T>

    override fun serialize(t: T?): ByteArray {
        return t?.toByteArray() ?: ByteArray(0)
    }

    override fun deserialize(bytes: ByteArray?): T? {
        if (bytes == null || bytes.isEmpty()) return null
        return parser.parseFrom(bytes)
    }
}
