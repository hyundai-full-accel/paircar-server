package com.fullaccel.paircarserver.car.application.dto

import com.fullaccel.paircarserver.car.domain.Car
import com.fullaccel.paircarserver.car.domain.FuelType
import java.math.BigDecimal

data class CarResult(
    val carId: Long,
    val vehicleKey: String,
    val fuelType: FuelType,
    val manualFuelEconomy: BigDecimal?,
) {
    companion object {
        fun from(car: Car) = CarResult(
            carId = car.id,
            vehicleKey = car.vehicleKey,
            fuelType = car.fuelType,
            manualFuelEconomy = car.manualFuelEconomy,
        )
    }
}
