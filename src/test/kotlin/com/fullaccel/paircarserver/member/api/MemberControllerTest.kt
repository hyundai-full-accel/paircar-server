package com.fullaccel.paircarserver.member.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fullaccel.paircarserver.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@IntegrationTest
@AutoConfigureMockMvc
class MemberControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    private fun createMember(nickname: String): String {
        val body = mockMvc.post("/api/members") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"nickname": "$nickname"}"""
        }.andReturn().response.contentAsString
        return objectMapper.readTree(body)["data"]["token"].asText()
    }

    @Test
    fun `멤버를 만들면 토큰을 주고 그 토큰으로 내 정보를 조회한다`() {
        mockMvc.post("/api/members") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"nickname": "지훈"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.nickname") { value("지훈") }
            jsonPath("$.data.token") { isNotEmpty() }
        }

        val token = createMember("수아")
        mockMvc.get("/api/members/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.nickname") { value("수아") }
        }
    }

    @Test
    fun `토큰이 없거나 틀리면 401 UNAUTHORIZED`() {
        mockMvc.get("/api/members/me").andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("UNAUTHORIZED") }
        }
        mockMvc.get("/api/members/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer wrong-token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `닉네임이 비어 있으면 INVALID_INPUT`() {
        mockMvc.post("/api/members") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"nickname": ""}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
            jsonPath("$.error.fieldErrors[0].field") { value("nickname") }
        }
    }

    @Test
    fun `계좌를 등록하고 조회한다`() {
        val token = createMember("지훈")

        mockMvc.get("/api/members/me/bank-account") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { doesNotExist() }
        }

        mockMvc.put("/api/members/me/bank-account") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"bankName": "○○은행", "accountNumber": "3333-01-1234567", "accountHolder": "지훈"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.accountNumber") { value("3333-01-1234567") }
        }
    }

    @Test
    fun `Swagger 문서가 열린다`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.info.title") { value("페어카 크루 서버 API") }
        }
    }
}
