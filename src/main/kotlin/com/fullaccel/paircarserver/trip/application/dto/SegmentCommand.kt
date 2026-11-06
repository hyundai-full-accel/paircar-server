package com.fullaccel.paircarserver.trip.application.dto

import com.fullaccel.paircarserver.trip.domain.FuelEconomySource
import java.math.BigDecimal

data class SegmentCommand(
    val seq: Int, // 1부터
    val startOdometerKm: BigDecimal,
    val endOdometerKm: BigDecimal,
    val fuelEconomy: BigDecimal, // km/L, EV는 km/kWh
    val fuelEconomySource: FuelEconomySource,
    // 이 구간에 타고 있던 동승자 크루 멤버. 운전자 혼자 달린 구간이면 빈 목록
    val riderCrewMemberIds: List<Long>,
    val startPointIndex: Int? = null,
    val endPointIndex: Int? = null,
)
