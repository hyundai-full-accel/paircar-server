package com.fullaccel.paircarserver.global.config

import com.fullaccel.paircarserver.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options

// 허용 출처는 application-test.yaml의 paircar.cors.allowed-origins
@IntegrationTest
@AutoConfigureMockMvc
class CorsConfigTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @Test
    fun `동승자 웹 출처의 preflight를 허용한다`() {
        mockMvc.options("/api/members/me") {
            header(HttpHeaders.ORIGIN, WEB_ORIGIN)
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization")
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, WEB_ORIGIN) }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "authorization") }
            header { doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) }
        }
    }

    @Test
    fun `오류 응답에도 CORS 헤더가 붙어 웹이 오류 코드를 읽을 수 있다`() {
        mockMvc.get("/api/members/me") {
            header(HttpHeaders.ORIGIN, WEB_ORIGIN)
        }.andExpect {
            status { isUnauthorized() }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, WEB_ORIGIN) }
            jsonPath("$.error.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `허용하지 않은 출처의 preflight는 거절한다`() {
        mockMvc.options("/api/members/me") {
            header(HttpHeaders.ORIGIN, "https://evil.example")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        }.andExpect {
            status { isForbidden() }
            header { doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) }
        }
    }

    companion object {
        private const val WEB_ORIGIN = "https://web.paircar.test"
    }
}
