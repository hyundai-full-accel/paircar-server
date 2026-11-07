package com.fullaccel.paircarserver.crew.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// 크루 멤버 한 명의 그날 출석. 기록이 없으면 기본값(탐, 기본 탈 곳)
data class AttendanceResult(
    val crewMemberId: Long,
    val memberId: Long,
    val nickname: String,
    val serviceDate: LocalDate,
    val attending: Boolean,
    @field:Schema(description = "그날 탈 곳. 그날만 바꾼 곳이 있으면 그곳, 없으면 기본 탈 곳")
    val boardPlace: CrewPlaceResult?,
    @field:Schema(description = "그날 내릴 곳 (기본 내릴 곳)")
    val alightPlace: CrewPlaceResult?,
    @field:Schema(description = "출발 전에 알린 그날 지각 (분)", example = "10")
    val delayMinutes: Int?,
    @field:Schema(description = "그날 출석을 따로 정했는지. false면 기본값")
    val changed: Boolean,
)
