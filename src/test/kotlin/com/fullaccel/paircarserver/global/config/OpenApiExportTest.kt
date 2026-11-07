package com.fullaccel.paircarserver.global.config

import com.fullaccel.paircarserver.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.io.File
import kotlin.test.assertTrue

// 서버를 띄우지 않고 API 명세(OpenAPI)를 build/openapi.json으로 내보냄
// 동승자 웹은 이 파일로 API 타입을 만듦 (paircar-mobile: npm run gen:api)
@IntegrationTest
@AutoConfigureMockMvc
class OpenApiExportTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @Test
    fun `API 명세를 파일로 내보낸다`() {
        val spec = mockMvc.get("/v3/api-docs").andReturn().response.getContentAsString(Charsets.UTF_8)

        assertTrue(spec.contains("\"openapi\""))
        File("build/openapi.json").apply { parentFile.mkdirs() }.writeText(spec)
    }
}
