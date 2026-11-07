package com.fullaccel.paircarserver.crew.application.dto

import com.fullaccel.paircarserver.crew.domain.SettlementMode
import java.time.DayOfWeek
import java.time.LocalTime

// 크루 설정 전체. null은 "그 값을 지움"
data class UpdateCrewCommand(
    val name: String,
    val departureTime: LocalTime?,
    val departureDays: Set<DayOfWeek>,
    val originPlaceId: Long?,
    val destinationPlaceId: Long?,
    val settlementMode: SettlementMode,
    val includeDriver: Boolean,
)

// 출석 변경. boardPlaceId가 null이면 기본 탈 곳
data class AttendanceCommand(
    val attending: Boolean,
    val boardPlaceId: Long?,
)
