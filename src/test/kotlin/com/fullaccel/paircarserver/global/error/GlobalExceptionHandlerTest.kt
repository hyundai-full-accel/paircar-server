package com.fullaccel.paircarserver.global.error

import com.fullaccel.paircarserver.global.auth.TokenAuthenticator
import com.fullaccel.paircarserver.global.response.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(controllers = [ExceptionTestController::class])
@Import(GlobalExceptionHandler::class)
class GlobalExceptionHandlerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    // WebConfig의 토큰 리졸버가 필요로 함. 이 테스트는 인증을 쓰지 않음
    @MockitoBean
    private lateinit var tokenAuthenticator: TokenAuthenticator

    @Test
    fun `성공 응답은 success와 data만 담는다`() {
        mockMvc.get("/test/ok").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data") { value("ok") }
            jsonPath("$.error") { doesNotExist() }
        }
    }

    @Test
    fun `BusinessException은 ErrorCode의 상태와 코드로 응답한다`() {
        mockMvc.get("/test/business").andExpect {
            status { isConflict() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.data") { doesNotExist() }
            jsonPath("$.error.code") { value("DATA_CONFLICT") }
            jsonPath("$.error.message") { value("직접 지정한 메시지") }
            jsonPath("$.error.fieldErrors") { doesNotExist() }
        }
    }

    @Test
    fun `Valid 검증 실패는 필드별 원인을 담는다`() {
        mockMvc.post("/test/valid") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "", "count": 0}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
            jsonPath("$.error.fieldErrors.length()") { value(2) }
        }
    }

    @Test
    fun `non-null 필드가 빠진 JSON은 빠진 필드를 알려준다`() {
        mockMvc.post("/test/valid") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "수아"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_JSON") }
            jsonPath("$.error.fieldErrors[0].field") { value("count") }
        }
    }

    @Test
    fun `깨진 JSON은 INVALID_JSON으로 응답한다`() {
        mockMvc.post("/test/valid") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": """
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_JSON") }
        }
    }

    @Test
    fun `필수 헤더가 없으면 헤더 이름을 알려준다`() {
        mockMvc.get("/test/header").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
            jsonPath("$.error.fieldErrors[0].field") { value("X-Member-Id") }
        }
    }

    @Test
    fun `없는 경로는 RESOURCE_NOT_FOUND로 응답한다`() {
        mockMvc.get("/test/missing").andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("RESOURCE_NOT_FOUND") }
        }
    }

    @Test
    fun `지원하지 않는 메서드는 METHOD_NOT_ALLOWED로 응답한다`() {
        mockMvc.post("/test/ok").andExpect {
            status { isMethodNotAllowed() }
            jsonPath("$.error.code") { value("METHOD_NOT_ALLOWED") }
        }
    }

    @Test
    fun `예상하지 못한 예외는 내부 메시지를 숨기고 INTERNAL_ERROR로 응답한다`() {
        mockMvc.get("/test/unexpected").andExpect {
            status { isInternalServerError() }
            jsonPath("$.error.code") { value("INTERNAL_ERROR") }
            jsonPath("$.error.message") { value(ErrorCode.INTERNAL_ERROR.message) }
        }
    }
}

@RestController
class ExceptionTestController {

    data class TestRequest(
        @field:NotBlank
        val name: String,
        @field:Positive
        val count: Int,
    )

    @GetMapping("/test/ok")
    fun ok(): ApiResponse<String> = ApiResponse.ok("ok")

    @GetMapping("/test/business")
    fun business(): ApiResponse<Unit> = throw BusinessException(ErrorCode.DATA_CONFLICT, "직접 지정한 메시지")

    @PostMapping("/test/valid")
    fun valid(@Valid @RequestBody request: TestRequest): ApiResponse<TestRequest> = ApiResponse.ok(request)

    @GetMapping("/test/header")
    fun header(@RequestHeader("X-Member-Id") memberId: Long): ApiResponse<Long> = ApiResponse.ok(memberId)

    @GetMapping("/test/unexpected")
    fun unexpected(): ApiResponse<Unit> = throw IllegalStateException("내부 정보")
}
