package com.fullaccel.paircarserver.car.application.dto

import com.fullaccel.paircarserver.car.domain.FuelType
import java.math.BigDecimal

// 차량 수정. null은 "그 값을 지움"
data class UpdateCarCommand(
    val fuelType: FuelType,
    val manualFuelEconomy: BigDecimal?,
    val manualFuelPriceWon: Int?,
)
