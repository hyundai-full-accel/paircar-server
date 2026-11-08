package com.fullaccel.paircarserver.global.logging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.support.bearer
import com.fullaccel.paircarserver.support.createMemberToken
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.assertTrue

@IntegrationTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension::class)
class RequestLoggingFilterTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @Test
    fun `nginx가 넘긴 요청 ID를 응답 헤더와 로그에 쓰고, 로그에 멤버 ID와 결과를 남긴다`(output: CapturedOutput) {
        val token = mockMvc.createMemberToken(objectMapper, "지훈")

        mockMvc.get("/api/members/me") {
            bearer(token)
            header("X-Request-Id", "abc123def456")
        }.andExpect {
            header { string("X-Request-Id", "abc123def456") }
        }

        val line = output.lines().last { it.contains("GET /api/members/me") }
        assertTrue(line.contains("[req=abc123def456 m="), line)
        assertTrue(Regex("m=\\d+]").containsMatchIn(line), line)
        assertTrue(line.contains("→ 200"), line)
    }

    @Test
    fun `요청 ID가 없거나 이상하면 새로 만든다`() {
        mockMvc.get("/api/members/me") { header("X-Request-Id", "bad id with spaces") }.andExpect {
            header { string("X-Request-Id", matchesPattern("[0-9a-f]{16}")) }
        }
    }

    @Test
    fun `SSE 구독 URL의 티켓은 로그에서 가린다`(output: CapturedOutput) {
        mockMvc.get("/api/live/events?ticket=secret-ticket-value")

        assertTrue(output.contains("/api/live/events?ticket=***"))
        assertTrue(!output.contains("secret-ticket-value"))
    }
}
