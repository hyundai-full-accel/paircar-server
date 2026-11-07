package com.fullaccel.paircarserver.trip.application.event

import java.math.BigDecimal

// 운행 모듈이 상태를 바꾼 뒤 발행하는 도메인 이벤트. 트랜잭션 커밋 뒤에 다른 모듈(실시간 알림 등)이 받음
// 받는 쪽이 운행 모듈을 다시 조회하지 않도록 알림에 필요한 값(멤버 ID, 닉네임)을 담음

data class TripStartedEvent(
    val tripId: Long,
    val driverMemberId: Long,
    val driverNickname: String,
    val riderMemberIds: List<Long>,
)

data class RiderBoardedEvent(
    val tripId: Long,
    val driverMemberId: Long,
    val riderMemberId: Long,
    val riderCrewMemberId: Long,
    val riderNickname: String,
    val byRider: Boolean = false, // 동승자가 "탔어요"로 직접 알림 (운전자에게도 알림)
)

data class TripEndedEvent(
    val tripId: Long,
    val riderMemberIds: List<Long>,
)

// "오늘은 안 타요". 동승자가 직접 했으면 운전자에게 알림 (byRider)
data class BoardingCanceledEvent(
    val tripId: Long,
    val driverMemberId: Long,
    val riderCrewMemberId: Long,
    val riderNickname: String,
    val byRider: Boolean,
)

// "오늘은 후문에서 탈게요". 동승자가 직접 했으면 운전자에게 알림 (byRider)
data class BoardPlaceChangedEvent(
    val tripId: Long,
    val driverMemberId: Long,
    val riderCrewMemberId: Long,
    val riderNickname: String,
    val placeId: Long,
    val placeName: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val byRider: Boolean,
)
