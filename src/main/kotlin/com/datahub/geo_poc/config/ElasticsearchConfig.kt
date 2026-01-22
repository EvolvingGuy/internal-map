package com.datahub.geo_poc.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.http.HttpHost
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
        ).build()

        val objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        val transport = RestClientTransport(restClient, JacksonJsonpMapper(objectMapper))
        return ElasticsearchClient(transport)
    }
}
