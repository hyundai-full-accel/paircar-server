package com.fullaccel.paircarserver.global.time

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

// "이번 달", "지난달" 같은 판단에 쓰는 시계. 테스트에서 날짜를 고정하려고 빈으로 둠
@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock = Clock.system(KST)
}
