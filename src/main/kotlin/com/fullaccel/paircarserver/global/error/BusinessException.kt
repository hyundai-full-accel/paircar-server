package com.fullaccel.paircarserver.global.error

// 서비스 로직에서 규칙 위반을 알릴 때 던지는 예외
// message를 넘기면 ErrorCode 기본 메시지 대신 응답에 나감
// data를 넘기면 실패 응답의 data에 실려 클라이언트가 복구에 씀 (예: 진행 중인 운행 ID)
class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    val data: Any? = null,
) : RuntimeException(message)
