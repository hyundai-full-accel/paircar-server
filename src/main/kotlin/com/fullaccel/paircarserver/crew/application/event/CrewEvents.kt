package com.fullaccel.paircarserver.crew.application.event

import java.math.BigDecimal

// 크루 모듈이 상태를 바꾼 뒤 발행하는 도메인 이벤트. 트랜잭션 커밋 뒤에 다른 모듈이 받음

// 우회 거리가 새로 등록되거나 값이 바뀌어 운전자, 동승자의 확인이 필요함
data class DetourConfirmationRequestedEvent(
    val detourId: Long,
    val crewId: Long,
    val passengerMemberId: Long,
    val passengerNickname: String,
    val driverMemberId: Long,
    val driverNickname: String,
    val placeName: String,
    val detourKm: BigDecimal,
)
