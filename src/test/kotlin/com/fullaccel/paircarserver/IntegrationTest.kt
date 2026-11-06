package com.fullaccel.paircarserver

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

// 실제 PostgreSQL(Testcontainers)로 도는 통합 테스트. 테스트마다 트랜잭션을 롤백함
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
@Transactional
annotation class IntegrationTest
