package com.fullaccel.paircarserver.crew.api.dto

import com.fullaccel.paircarserver.crew.application.dto.RegisterDetourCommand
import com.fullaccel.paircarserver.crew.application.dto.RegisterPlaceCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateCrewRequest(
    @field:Schema(example = "판교 출근 크루")
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,
)

data class ChangeFuelPriceRequest(
    @field:Schema(description = "직접 입력 유가 (원/L, EV는 원/kWh). null이면 해제하고 공공 유가를 씀", example = "1684", nullable = true)
    @field:Positive
    val priceWon: Int?,
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
