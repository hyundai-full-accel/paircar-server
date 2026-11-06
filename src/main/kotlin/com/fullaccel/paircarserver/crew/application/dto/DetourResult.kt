package com.fullaccel.paircarserver.crew.application.dto

import com.fullaccel.paircarserver.crew.domain.CrewMemberDetour
import java.math.BigDecimal
import java.time.Instant

data class DetourResult(
    val detourId: Long,
    val passengerId: Long,
    val driverId: Long,
    val placeId: Long,
    val detourKm: BigDecimal,
    val driverConfirmedAt: Instant?,
    val passengerConfirmedAt: Instant?,
    val confirmed: Boolean,
) {
    companion object {
        fun from(detour: CrewMemberDetour) = DetourResult(
            detourId = detour.id,
            passengerId = detour.passenger.id,
            driverId = detour.driver.id,
            placeId = detour.place.id,
            detourKm = detour.detourKm,
            driverConfirmedAt = detour.driverConfirmedAt,
            passengerConfirmedAt = detour.passengerConfirmedAt,
            confirmed = detour.isConfirmed,
        )
    }
}
