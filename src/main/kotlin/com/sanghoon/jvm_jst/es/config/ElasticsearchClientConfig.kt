package com.sanghoon.jvm_jst.es.config

import com.fasterxml.jackson.databind.ObjectMapper
import co.elastic.clients.json.JsonpMapper
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration

/**
 * Spring Boot 4.0 + Spring Data Elasticsearch 6.0 + ES 9.x
 * Rest5Client 기반 설정
 */
@Configuration
class ElasticsearchClientConfig(
    private val objectMapper: ObjectMapper
) : ElasticsearchConfiguration() {

    @Value("\${elasticsearch.host:localhost}")
    private lateinit var host: String

    @Value("\${elasticsearch.port:9200}")
    private var port: Int = 9200

    override fun clientConfiguration(): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo("$host:$port")
            .build()
    }

    override fun jsonpMapper(): JsonpMapper {
        return JacksonJsonpMapper(objectMapper)
    }
}
