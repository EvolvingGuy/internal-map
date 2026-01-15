1. build.gradle.kts

plugins {
id("com.google.protobuf") version "0.9.4"
}

dependencies {
implementation("com.google.protobuf:protobuf-kotlin:4.29.3")
}

protobuf {
protoc {
artifact = "com.google.protobuf:protoc:4.29.3"
}
}

2. proto 파일 (src/main/proto/pnu_agg.proto)

syntax = "proto3";

package com.sanghoon.jvm_jst.pnu.proto;

option java_multiple_files = true;

message AggCacheDataProto {
int64 code = 1;
int32 cnt = 2;
double sum_lat = 3;
double sum_lng = 4;
}

message AggCacheListProto {
repeated AggCacheDataProto items = 1;
}

3. ProtobufRedisSerializer

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

4. RedisTemplate 빈

@Bean
fun aggCacheRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, AggCacheListProto> {
return RedisTemplate<String, AggCacheListProto>().apply {
connectionFactory = factory
keySerializer = StringRedisSerializer()
valueSerializer = ProtobufRedisSerializer(AggCacheListProto.getDefaultInstance())
}
}

빌드

./gradlew build
