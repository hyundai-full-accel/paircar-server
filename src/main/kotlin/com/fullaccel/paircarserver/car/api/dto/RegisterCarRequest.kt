package com.fullaccel.paircarserver.car.api.dto

import com.fullaccel.paircarserver.car.application.dto.RegisterCarCommand
import com.fullaccel.paircarserver.car.domain.FuelType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class RegisterCarRequest(
    @field:Schema(description = "Pleos Connect 차량 식별값. 같은 값이면 같은 차량으로 봄", example = "PLEOS-VEHICLE-0001")
    @field:NotBlank
    @field:Size(max = 64)
    val vehicleKey: String,

    @field:Schema(description = "연료 종류", example = "GASOLINE")
    val fuelType: FuelType,

    @field:Schema(description = "입력 연비 (km/L, EV는 km/kWh). 실측 연비를 못 쓰는 차종만 입력", example = "13.20", nullable = true)
    @field:DecimalMin(value = "0.01")
    @field:Digits(integer = 3, fraction = 2)
    val manualFuelEconomy: BigDecimal? = null,
) {
    fun toCommand() = RegisterCarCommand(vehicleKey, fuelType, manualFuelEconomy)
}
