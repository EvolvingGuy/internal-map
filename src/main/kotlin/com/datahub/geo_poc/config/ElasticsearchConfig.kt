package com.datahub.geo_poc.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticsearchConfig(
    @Value("\${spring.elasticsearch.uris}") private val esUri: String
) {
    @Bean
    fun elasticsearchClient(): ElasticsearchClient {
        val uri = java.net.URI.create(esUri)
        val restClient = RestClient.builder(
            HttpHost(uri.host, uri.port, uri.scheme)
        ).setHttpClientConfigCallback { httpClientBuilder ->
            httpClientBuilder
                // 커넥션 풀: 30개로 제한하여 소켓 재사용
                .setMaxConnTotal(30)
                .setMaxConnPerRoute(30)
                // Keep-Alive: 30초 유지 (소켓이 매번 닫히는 것 방지)
                .setKeepAliveStrategy { _, _ -> 30 * 1000L }
                // 타임아웃 설정
                .setDefaultRequestConfig(
                    RequestConfig.custom()
                        .setConnectTimeout(5000)
                        .setSocketTimeout(60000)
                        .build()
                )
        }.build()

        val objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        val transport = RestClientTransport(restClient, JacksonJsonpMapper(objectMapper))
        return ElasticsearchClient(transport)
    }
}
