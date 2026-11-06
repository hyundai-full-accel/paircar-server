package com.fullaccel.paircarserver.global.error

// 서비스 로직에서 규칙 위반을 알릴 때 던지는 예외
// message를 넘기면 ErrorCode 기본 메시지 대신 응답에 나감
class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
) : RuntimeException(message)
