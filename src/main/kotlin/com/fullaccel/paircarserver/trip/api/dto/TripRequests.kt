package com.fullaccel.paircarserver.trip.api.dto

import com.fullaccel.paircarserver.trip.application.dto.BoardingEventCommand
import com.fullaccel.paircarserver.trip.application.dto.EndTripCommand
import com.fullaccel.paircarserver.trip.application.dto.RiderCommand
import com.fullaccel.paircarserver.trip.application.dto.SegmentCommand
import com.fullaccel.paircarserver.trip.application.dto.StartTripCommand
import com.fullaccel.paircarserver.trip.domain.FuelEconomySource
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.Instant

data class StartTripRequest(
    val crewId: Long,
    val carId: Long,
    @field:Schema(description = "출발지 장소 ID")
    val originPlaceId: Long,
    @field:Schema(description = "목적지 장소 ID (내비 목적지)")
    val destinationPlaceId: Long,
    @field:Schema(description = "출발 시각 (시동)", example = "2026-11-09T08:00:00+09:00")
    val startedAt: Instant,
    @field:Schema(description = "출발 시점 누적 주행거리 (km)", example = "10000.0")
    @field:DecimalMin("0.0")
    @field:Digits(integer = 8, fraction = 1)
    val startOdometerKm: BigDecimal,
    @field:Schema(description = "이번 운행에 탈 동승자. 응답의 waypoints로 경유지를 설정하세요")
    @field:Valid
    val riders: List<RiderRequest> = emptyList(),
) {
    fun toCommand() = StartTripCommand(
        crewId = crewId,
        carId = carId,
        originPlaceId = originPlaceId,
        destinationPlaceId = destinationPlaceId,
        startedAt = startedAt,
        startOdometerKm = startOdometerKm,
        riders = riders.map { RiderCommand(it.crewMemberId, it.boardPlaceId, it.alightPlaceId) },
    )
}

data class RiderRequest(
    @field:Schema(description = "동승자 크루 멤버 ID")
    val crewMemberId: Long,
    @field:Schema(description = "탈 예정 장소 ID")
    val boardPlaceId: Long,
    @field:Schema(description = "내릴 예정 장소 ID")
    val alightPlaceId: Long,
)

data class BoardingEventRequest(
    @field:Schema(description = "차량 신호 기준 발생 시각", example = "2026-11-09T08:10:00+09:00")
    val occurredAt: Instant,
    @field:Schema(description = "발생 시점 누적 주행거리 (km)", example = "10005.0")
    @field:DecimalMin("0.0")
    @field:Digits(integer = 8, fraction = 1)
    val odometerKm: BigDecimal,
) {
    fun toCommand() = BoardingEventCommand(occurredAt, odometerKm)
}

data class ChangeBoardPlaceRequest(
    @field:Schema(description = "새로 탈 장소 ID")
    val placeId: Long,
)

data class UploadSegmentsRequest(
    @field:NotEmpty
    @field:Valid
    val segments: List<SegmentRequest>,
) {
    fun toCommands() = segments.map { it.toCommand() }
}

data class SegmentRequest(
    @field:Schema(description = "운행 안에서 구간 순서 (1부터)", example = "1")
    @field:Min(1)
    val seq: Int,
    @field:Schema(example = "10000.0")
    @field:DecimalMin("0.0")
    @field:Digits(integer = 8, fraction = 1)
    val startOdometerKm: BigDecimal,
    @field:Schema(example = "10005.0")
    @field:DecimalMin("0.0")
    @field:Digits(integer = 8, fraction = 1)
    val endOdometerKm: BigDecimal,
    @field:Schema(description = "구간 평균 연비 (km/L, EV는 km/kWh)", example = "13.20")
    @field:DecimalMin("0.01")
    @field:Digits(integer = 3, fraction = 2)
    val fuelEconomy: BigDecimal,
    @field:Schema(description = "MEASURED: 차량 실측, MANUAL: 입력 연비", example = "MEASURED")
    val fuelEconomySource: FuelEconomySource,
    @field:Schema(description = "이 구간에 타고 있던 동승자 크루 멤버 ID. 운전자 혼자면 빈 배열")
    val riderCrewMemberIds: List<Long> = emptyList(),
    @field:Schema(description = "운행 경로(routePolyline)에서 구간 시작 점 번호", nullable = true)
    @field:PositiveOrZero
    val startPointIndex: Int? = null,
    @field:Schema(description = "운행 경로(routePolyline)에서 구간 끝 점 번호", nullable = true)
    @field:PositiveOrZero
    val endPointIndex: Int? = null,
) {
    fun toCommand() = SegmentCommand(
        seq = seq,
        startOdometerKm = startOdometerKm,
        endOdometerKm = endOdometerKm,
        fuelEconomy = fuelEconomy,
        fuelEconomySource = fuelEconomySource,
        riderCrewMemberIds = riderCrewMemberIds,
        startPointIndex = startPointIndex,
        endPointIndex = endPointIndex,
    )
}

data class EndTripRequest(
    @field:Schema(description = "도착 시각", example = "2026-11-09T08:30:00+09:00")
    val endedAt: Instant,
    @field:Schema(description = "도착 시점 누적 주행거리 (km)", example = "10017.0")
    @field:DecimalMin("0.0")
    @field:Digits(integer = 8, fraction = 1)
    val endOdometerKm: BigDecimal,
    @field:Schema(description = "그날 통행료 (원). RouteStartInfo.tollFare", example = "0")
    @field:PositiveOrZero
    val tollFareWon: Int = 0,
    @field:Schema(description = "운행 전체 실제 주행 경로 (Encoded Polyline)", nullable = true)
    val routePolyline: String? = null,
) {
    fun toCommand() = EndTripCommand(endedAt, endOdometerKm, tollFareWon, routePolyline)
}
