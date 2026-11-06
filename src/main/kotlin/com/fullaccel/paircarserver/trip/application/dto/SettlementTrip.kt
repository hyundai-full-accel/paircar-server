package com.fullaccel.paircarserver.trip.application.dto

import com.fullaccel.paircarserver.car.domain.FuelType
import com.fullaccel.paircarserver.trip.domain.FuelEconomySource
import java.math.BigDecimal
import java.time.LocalDate

// 정산에 필요한 운행 정보 (끝난 운행, 실제 탑승한 동승자만)
data class SettlementTrip(
    val tripId: Long,
    val driverCrewMemberId: Long,
    val fuelType: FuelType,
    val serviceDate: LocalDate,
    val originPlaceId: Long,
    val destinationPlaceId: Long,
    val tollFareWon: Int,
    val segments: List<SettlementSegment>,
    val boardings: List<SettlementBoarding>,
)

data class SettlementSegment(
    val distanceKm: BigDecimal,
    val fuelEconomy: BigDecimal,
    val fuelEconomySource: FuelEconomySource,
    val riderBoardingIds: Set<Long>,
)

data class SettlementBoarding(
    val boardingId: Long,
    val crewMemberId: Long,
    val boardPlaceId: Long,
    val alightPlaceId: Long,
)
