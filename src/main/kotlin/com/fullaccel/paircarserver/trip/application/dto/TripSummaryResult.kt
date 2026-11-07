package com.fullaccel.paircarserver.trip.application.dto

import com.fullaccel.paircarserver.trip.domain.Boarding
import com.fullaccel.paircarserver.trip.domain.Trip
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// 운행 목록 한 줄 (동승자 웹 탑승 기록, 차량 앱 운행 기록 탭). 구간은 빼고 누가 어디서 탔는지만
data class TripSummaryResult(
    val tripId: Long,
    val serviceDate: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant?,
    val driverCrewMemberId: Long,
    val driverNickname: String,
    val originName: String,
    val destinationName: String,
    @field:Schema(description = "운행 거리 (km). 끝난 운행만", example = "21.4")
    val distanceKm: BigDecimal?,
    @field:Schema(description = "동승자 (불참 처리된 사람은 빠짐)")
    val riders: List<RiderSummary>,
) {
    companion object {
        fun of(trip: Trip, boardings: List<Boarding>) = TripSummaryResult(
            tripId = trip.id,
            serviceDate = trip.serviceDate,
            startedAt = trip.startedAt,
            endedAt = trip.endedAt,
            driverCrewMemberId = trip.driver.id,
            driverNickname = trip.driver.member.nickname,
            originName = trip.originPlace.name,
            destinationName = trip.destinationPlace.name,
            distanceKm = trip.endOdometerKm?.minus(trip.startOdometerKm),
            riders = boardings.map(RiderSummary::from),
        )
    }
}

data class RiderSummary(
    @field:Schema(description = "영수증 하루치 줄(ReceiptItemResult.boardingId)과 맞출 때 씀")
    val boardingId: Long,
    val crewMemberId: Long,
    val nickname: String,
    val boardPlaceName: String,
    val alightPlaceName: String,
    val boardedAt: Instant?,
    val alightedAt: Instant?,
    @field:Schema(description = "탄 거리 (km) = 하차 주행거리 - 탑승 주행거리. 내린 뒤에만", example = "12.0")
    val rideDistanceKm: BigDecimal?,
) {
    companion object {
        fun from(boarding: Boarding) = RiderSummary(
            boardingId = boarding.id,
            crewMemberId = boarding.crewMember.id,
            nickname = boarding.crewMember.member.nickname,
            boardPlaceName = boarding.boardPlace.name,
            alightPlaceName = boarding.alightPlace.name,
            boardedAt = boarding.boardedAt,
            alightedAt = boarding.alightedAt,
            rideDistanceKm = boarding.boardOdometerKm?.let { board -> boarding.alightOdometerKm?.minus(board) },
        )
    }
}
