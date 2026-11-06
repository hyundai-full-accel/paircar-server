package com.fullaccel.paircarserver.trip.application.dto

import java.math.BigDecimal
import java.time.Instant

// 탑승, 하차 감지 이벤트 (차량 신호 기준 시각과 누적 주행거리)
data class BoardingEventCommand(
    val occurredAt: Instant,
    val odometerKm: BigDecimal,
)
