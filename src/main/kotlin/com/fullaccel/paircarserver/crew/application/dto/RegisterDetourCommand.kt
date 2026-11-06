package com.fullaccel.paircarserver.crew.application.dto

import java.math.BigDecimal

data class RegisterDetourCommand(
    val passengerId: Long, // 동승자 크루 멤버 ID
    val driverId: Long,    // 운전자 크루 멤버 ID
    val placeId: Long,     // 경유하는 장소 (승차 지점 또는 하차 지점)
    val detourKm: BigDecimal,
)
