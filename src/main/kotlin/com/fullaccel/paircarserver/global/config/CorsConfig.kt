package com.fullaccel.paircarserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import java.time.Duration

// 동승자 웹은 서버와 다른 도메인에 따로 배포하므로 그 출처만 /api 호출을 허용
// 필터로 걸어 컨트롤러 밖에서 난 오류(404, 인증 실패) 응답에도 CORS 헤더가 붙게 함
@Configuration
class CorsConfig {

    @Bean
    fun corsFilter(properties: CorsProperties): CorsFilter {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = properties.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE")
            allowedHeaders = listOf(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE)
            allowCredentials = false // 쿠키를 쓰지 않음. 인증은 Authorization 헤더의 기기 토큰
            maxAge = Duration.ofHours(1).seconds // preflight 결과를 브라우저가 1시간 기억
        }
        return CorsFilter(UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/api/**", configuration) })
    }
}

// 허용할 동승자 웹 주소 (환경변수 PAIRCAR_CORS_ALLOWED_ORIGINS, 쉼표로 여러 개)
@ConfigurationProperties("paircar.cors")
data class CorsProperties(
    val allowedOrigins: List<String>,
)
