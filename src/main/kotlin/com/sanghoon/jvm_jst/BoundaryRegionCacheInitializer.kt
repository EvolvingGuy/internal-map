package com.sanghoon.jvm_jst

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * BoundaryRegionCache 초기화 완료 이벤트
 */
class BoundaryRegionCacheReadyEvent(source: Any) : org.springframework.context.ApplicationEvent(source)

@Component
class BoundaryRegionCacheInitializer(
    private val eventPublisher: ApplicationEventPublisher
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        log.info("BoundaryRegionCache 초기화 시작")
        val startTime = System.currentTimeMillis()

        BoundaryRegionCache.initialize()

        val elapsed = System.currentTimeMillis() - startTime
        log.info("BoundaryRegionCache 초기화 완료: ${BoundaryRegionCache.getRegionCount()}개 로드, ${elapsed}ms 소요")
        log.info("레벨별 개수: ${BoundaryRegionCache.getCountByLevel()}")

        // 초기화 완료 이벤트 발행
        eventPublisher.publishEvent(BoundaryRegionCacheReadyEvent(this))
    }
}
