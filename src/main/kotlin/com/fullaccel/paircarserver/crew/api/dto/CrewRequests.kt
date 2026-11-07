package com.fullaccel.paircarserver.crew.api.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fullaccel.paircarserver.crew.application.dto.AttendanceCommand
import com.fullaccel.paircarserver.crew.application.dto.RegisterDetourCommand
import com.fullaccel.paircarserver.crew.application.dto.UpdateCrewCommand
import com.fullaccel.paircarserver.crew.domain.SettlementMode
import com.fullaccel.paircarserver.crew.application.dto.RegisterPlaceCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime

data class CreateCrewRequest(
    @field:Schema(example = "판교 출근 크루")
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,
)

data class JoinAsNewMemberRequest(
    @field:Schema(description = "크루 안에서 보이는 이름", example = "수아")
    @field:NotBlank
    @field:Size(max = 30)
    val nickname: String,
)

data class RegisterPlaceRequest(
    @field:Schema(example = "○○아파트 정문")
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,

    @field:Schema(example = "37.401234")
    @field:DecimalMin("-90")
    @field:DecimalMax("90")
    @field:Digits(integer = 3, fraction = 6)
    val latitude: BigDecimal,

    @field:Schema(example = "127.108765")
    @field:DecimalMin("-180")
    @field:DecimalMax("180")
    @field:Digits(integer = 3, fraction = 6)
    val longitude: BigDecimal,
) {
    fun toCommand() = RegisterPlaceCommand(name, latitude, longitude)
}

data class RegisterDetourRequest(
    @field:Schema(description = "동승자 크루 멤버 ID", example = "2")
    val passengerId: Long,

    @field:Schema(description = "운전자 크루 멤버 ID", example = "1")
    val driverId: Long,

    @field:Schema(description = "경유하는 장소 ID (승차 지점 또는 하차 지점)", example = "3")
    val placeId: Long,

    @field:Schema(description = "우회 거리 (km). 경유 거리 - 직행 거리", example = "3.0")
    @field:DecimalMin("0.0")
    @field:Digits(integer = 5, fraction = 1)
    val detourKm: BigDecimal,
) {
    fun toCommand() = RegisterDetourCommand(passengerId, driverId, placeId, detourKm)
}

// 바꿀 수 있는 값 전체를 보냄. null은 그 값을 지움
data class UpdateCrewRequest(
    @field:Schema(example = "판교 출근 크루")
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,

    @field:Schema(description = "평소 출발 시각 (KST, HH:mm)", type = "string", example = "08:00", nullable = true)
    @field:JsonFormat(pattern = "HH:mm")
    val departureTime: LocalTime? = null,

    @field:Schema(description = "평소 도착 시각 (KST, HH:mm)", type = "string", example = "08:32", nullable = true)
    @field:JsonFormat(pattern = "HH:mm")
    val arrivalTime: LocalTime? = null,

    @field:Schema(description = "카풀하는 요일", example = "[\"MONDAY\", \"TUESDAY\", \"WEDNESDAY\", \"THURSDAY\", \"FRIDAY\"]")
    val departureDays: Set<DayOfWeek> = emptySet(),

    @field:Schema(description = "기본 출발지 장소 ID", example = "1", nullable = true)
    val originPlaceId: Long? = null,

    @field:Schema(description = "기본 목적지 장소 ID", example = "4", nullable = true)
    val destinationPlaceId: Long? = null,

    @field:Schema(description = "정산 방식. FIXED_MONTHLY는 아직 지원하지 않음", example = "FAIR")
    val settlementMode: SettlementMode,

    @field:Schema(description = "구간 탑승 인원에 운전자를 포함할지", example = "true")
    val includeDriver: Boolean,
) {
    fun toCommand() =
        UpdateCrewCommand(name, departureTime, arrivalTime, departureDays, originPlaceId, destinationPlaceId, settlementMode, includeDriver)
}

data class ChangeMemberPlacesRequest(
    @field:Schema(description = "평소 탈 곳 장소 ID. null이면 지움", example = "2", nullable = true)
    val boardPlaceId: Long? = null,

    @field:Schema(description = "평소 내릴 곳 장소 ID. null이면 지움", example = "4", nullable = true)
    val alightPlaceId: Long? = null,

    @field:Schema(description = "평소 픽업 시각 (KST, HH:mm). null이면 지움", type = "string", example = "07:32", nullable = true)
    @field:JsonFormat(pattern = "HH:mm")
    val pickupTime: LocalTime? = null,
)

data class AttendanceRequest(
    @field:Schema(description = "그날 타는지", example = "false")
    val attending: Boolean,

    @field:Schema(description = "그날만 다른 탈 곳 장소 ID. null이면 기본 탈 곳", example = "3", nullable = true)
    val boardPlaceId: Long? = null,

    @field:Schema(description = "출발 전에 알리는 그날 지각 (분, 1~120). 안 타는 날이면 무시", example = "10", nullable = true)
    @field:Min(1)
    @field:Max(120)
    val delayMinutes: Int? = null,
) {
    fun toCommand() = AttendanceCommand(attending, boardPlaceId, delayMinutes.takeIf { attending })
}
