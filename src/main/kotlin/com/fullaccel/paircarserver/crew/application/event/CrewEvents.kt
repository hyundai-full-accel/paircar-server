package com.fullaccel.paircarserver.crew.application.event

import java.math.BigDecimal
import java.time.LocalDate

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

// 크루 멤버가 그날 출석을 바꿈 (안 타요, 다시 타요, 그날만 다른 곳에서 타요, N분 늦어요)
// 출발 전이라 그날 운전자가 정해지지 않았으므로 바꾼 사람을 뺀 크루 멤버 전원에게 알림
data class AttendanceChangedEvent(
    val crewId: Long,
    val crewMemberId: Long,
    val nickname: String,
    val serviceDate: LocalDate,
    val attending: Boolean,
    val boardPlace: AttendancePlace?,
    val delayMinutes: Int?,
    val recipientMemberIds: List<Long>,
)

data class AttendancePlace(
    val placeId: Long,
    val name: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
)
