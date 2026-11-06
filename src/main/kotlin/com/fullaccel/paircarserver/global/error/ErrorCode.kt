package com.fullaccel.paircarserver.global.error

import org.springframework.http.HttpStatus

// 응답의 error.code로 나가는 값. 도메인 에러는 기능을 만들 때 아래에 추가함
// 재시도 기준: 5xx만 재시도. 4xx는 같은 요청을 다시 보내도 실패하므로 버림
enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "이미 존재하거나 다른 데이터와 충돌합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 멤버
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),
}
