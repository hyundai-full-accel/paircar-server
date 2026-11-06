package com.fullaccel.paircarserver.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post

// 컨트롤러 테스트 공통 도우미

// 멤버를 만들고 토큰을 돌려줌
fun MockMvc.createMemberToken(objectMapper: ObjectMapper, nickname: String): String =
    post("/api/members") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"nickname": "$nickname"}"""
    }.data(objectMapper)["token"].asText()

fun MockHttpServletRequestDsl.bearer(token: String) {
    header(HttpHeaders.AUTHORIZATION, "Bearer $token")
}

fun MockHttpServletRequestDsl.json(body: String) {
    contentType = MediaType.APPLICATION_JSON
    content = body
}

// 응답 본문의 data
fun ResultActionsDsl.data(objectMapper: ObjectMapper): JsonNode =
    objectMapper.readTree(andReturn().response.contentAsString)["data"]
