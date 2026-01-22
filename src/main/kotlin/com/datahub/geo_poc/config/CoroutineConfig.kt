package com.datahub.geo_poc.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig {

    companion object {
        const val INDEXING_PARALLELISM = 100
    }

    @Bean
    fun indexingDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO.limitedParallelism(INDEXING_PARALLELISM)
    }
}
