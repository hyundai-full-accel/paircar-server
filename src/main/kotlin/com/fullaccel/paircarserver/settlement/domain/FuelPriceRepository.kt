package com.fullaccel.paircarserver.settlement.domain

import com.fullaccel.paircarserver.car.domain.FuelType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface FuelPriceRepository : JpaRepository<FuelPrice, Long> {

    fun findByFuelTypeAndMonth(fuelType: FuelType, month: LocalDate): FuelPrice?
}
