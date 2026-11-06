package com.fullaccel.paircarserver.trip.application.dto

import java.math.BigDecimal
import java.time.Instant

data class EndTripCommand(
    val endedAt: Instant,
    val endOdometerKm: BigDecimal,
    val tollFareWon: Int = 0,
    val routePolyline: String? = null,
)
