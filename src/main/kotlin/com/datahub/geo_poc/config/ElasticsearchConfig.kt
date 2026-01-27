package com.datahub.geo_poc.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.config.RequestConfig
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.ssl.SSLContextBuilder
import org.opensearch.client.RestClient
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.rest_client.RestClientTransport
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticsearchConfig(
    @Value("\${spring.elasticsearch.uris}") private val esUri: String,
    @Value("\${spring.elasticsearch.username:}") private val username: String,
    @Value("\${spring.elasticsearch.password:}") private val password: String
) {
    @Bean
    fun openSearchClient(): OpenSearchClient {
        val uri = java.net.URI.create(esUri)

        val restClientBuilder = RestClient.builder(
            HttpHost(uri.host, uri.port, uri.scheme)
        ).setHttpClientConfigCallback { httpClientBuilder ->
            // Basic Auth 설정 (username/password가 있을 경우)
            if (username.isNotBlank() && password.isNotBlank()) {
                val credentialsProvider = BasicCredentialsProvider().apply {
                    setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username, password))
                }
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            }

            // HTTPS 시 SSL 호스트명 검증 비활성화 (SSH 터널링용)
            if (uri.scheme == "https") {
                val sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial { _, _ -> true }
                    .build()
                httpClientBuilder
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            }

            httpClientBuilder
                .setMaxConnTotal(30)
                .setMaxConnPerRoute(30)
                .setKeepAliveStrategy { _, _ -> 30 * 1000L }
                .setDefaultRequestConfig(
                    RequestConfig.custom()
                        .setConnectTimeout(5000)
                        .setSocketTimeout(60000)
                        .build()
                )
        }

        val restClient = restClientBuilder.build()

        val objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        val transport = RestClientTransport(restClient, JacksonJsonpMapper(objectMapper))
        return OpenSearchClient(transport)
    }
}
