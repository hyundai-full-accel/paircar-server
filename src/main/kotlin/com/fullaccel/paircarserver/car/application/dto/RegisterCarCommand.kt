package com.fullaccel.paircarserver.car.application.dto

import com.fullaccel.paircarserver.car.domain.FuelType
import java.math.BigDecimal

data class RegisterCarCommand(
    val vehicleKey: String,
    val fuelType: FuelType,
    // 실측 연비를 못 쓰는 차종만 입력 (km/L, EV는 km/kWh)
    val manualFuelEconomy: BigDecimal? = null,
)
