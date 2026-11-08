package com.fullaccel.paircarserver.global.config

import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync
class AsyncConfig {

    // @Async(Web Push 발송)는 다른 스레드에서 돌아 로그 문맥(requestId, memberId)이 끊김. 넘길 때 복사함
    // Spring Boot가 이 빈을 기본 비동기 실행기에 붙임
    @Bean
    fun mdcTaskDecorator(): TaskDecorator = TaskDecorator { task ->
        val context = MDC.getCopyOfContextMap()
        Runnable {
            if (context != null) MDC.setContextMap(context) else MDC.clear()
            try {
                task.run()
            } finally {
                MDC.clear()
            }
        }
    }
}
