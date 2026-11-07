package com.fullaccel.paircarserver.car.api.dto

import com.fullaccel.paircarserver.car.application.dto.UpdateCarCommand
import com.fullaccel.paircarserver.car.domain.FuelType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

// 바꿀 수 있는 값 전체를 보냄. null은 그 값을 지움
data class UpdateCarRequest(
    @field:Schema(description = "연료 종류", example = "EV")
    val fuelType: FuelType,

    @field:Schema(description = "입력 연비 (km/L, EV는 km/kWh). 실측 연비를 쓰면 null", example = "5.80", nullable = true)
    @field:DecimalMin(value = "0.01")
    @field:Digits(integer = 3, fraction = 2)
    val manualFuelEconomy: BigDecimal? = null,

    @field:Schema(
        description = "직접 입력 유가 (원/L, EV는 원/kWh). 집 충전 단가처럼 공공 평균과 다를 때만. null이면 연료별 공공 월평균을 씀",
        example = "180",
        nullable = true,
    )
    @field:Positive
    val manualFuelPriceWon: Int? = null,
) {
    fun toCommand() = UpdateCarCommand(fuelType, manualFuelEconomy, manualFuelPriceWon)
}
