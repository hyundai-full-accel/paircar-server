package com.fullaccel.paircarserver.global.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

// 모든 API의 공통 응답 형식
// 성공: { "success": true, "data": ... }
// 실패: { "success": false, "error": { "code": ..., "message": ..., "fieldErrors": [...] } }
//       복구에 필요한 값이 있으면 data도 실림 (예: TRIP_ALREADY_IN_PROGRESS의 { "tripId": ... })
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorBody? = null,
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun ok(): ApiResponse<Unit> = ApiResponse(success = true)

        fun fail(error: ErrorBody, data: Any? = null): ApiResponse<Any> = ApiResponse(success = false, data = data, error = error)
    }
}

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ErrorBody(
    // ErrorCode 이름. 클라이언트는 메시지가 아니라 이 값으로 분기함
    val code: String,
    val message: String,
    // 요청 값 검증 실패 시 필드별 원인. 없으면 응답에서 빠짐
    @field:Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    val fieldErrors: List<FieldErrorBody> = emptyList(),
)

data class FieldErrorBody(
    val field: String,
    val reason: String,
)
