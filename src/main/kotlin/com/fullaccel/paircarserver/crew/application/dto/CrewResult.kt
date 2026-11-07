package com.fullaccel.paircarserver.crew.application.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fullaccel.paircarserver.crew.domain.Crew
import com.fullaccel.paircarserver.crew.domain.SettlementMode
import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalTime

data class CrewResult(
    val crewId: Long,
    val name: String,
    val settlementMode: SettlementMode,
    val includeDriver: Boolean,
    val monthlyFixedWon: Int?,
    @field:Schema(description = "평소 출발 시각 (KST, HH:mm)", type = "string", example = "08:00")
    @field:JsonFormat(pattern = "HH:mm")
    val departureTime: LocalTime?,
    @field:Schema(description = "카풀하는 요일")
    val departureDays: List<DayOfWeek>,
    @field:Schema(description = "기본 출발지")
    val originPlace: CrewPlaceResult?,
    @field:Schema(description = "기본 목적지")
    val destinationPlace: CrewPlaceResult?,
) {
    companion object {
        fun from(crew: Crew) = CrewResult(
            crewId = crew.id,
            name = crew.name,
            settlementMode = crew.settlementMode,
            includeDriver = crew.includeDriver,
            monthlyFixedWon = crew.monthlyFixedWon,
            departureTime = crew.departureTime,
            departureDays = crew.departureDays.sorted(),
            originPlace = crew.originPlace?.let(CrewPlaceResult::from),
            destinationPlace = crew.destinationPlace?.let(CrewPlaceResult::from),
        )
    }
}
