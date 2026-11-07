package com.fullaccel.paircarserver

import com.fullaccel.paircarserver.global.time.KST
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant

// 테스트의 "오늘"을 2027-01-15(KST)로 고정. 테스트 데이터의 2026년 11, 12월은 언제 돌려도 끝난 달
@TestConfiguration(proxyBeanMethods = false)
class TestClockConfiguration {

    @Bean
    @Primary
    fun fixedClock(): Clock = Clock.fixed(TODAY, KST)

    companion object {
        val TODAY: Instant = Instant.parse("2027-01-15T00:00:00Z")
    }
}
