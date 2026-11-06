package com.fullaccel.paircarserver.trip.application.dto

import java.math.BigDecimal
import java.time.Instant

data class StartTripCommand(
    val crewId: Long,
    val carId: Long,
    val originPlaceId: Long,
    val destinationPlaceId: Long,
    val startedAt: Instant,
    val startOdometerKm: BigDecimal,
    // 이번 운행에 탈 동승자. 운행 시작 때 탑승 행을 만들고 경유지를 정함
    val riders: List<RiderCommand>,
)

data class RiderCommand(
    val crewMemberId: Long,
    val boardPlaceId: Long,  // 탈 예정 지점
    val alightPlaceId: Long, // 내릴 예정 지점
)
