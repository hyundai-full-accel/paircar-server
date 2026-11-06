package com.fullaccel.paircarserver.trip.application.dto

import com.fullaccel.paircarserver.crew.domain.CrewPlace
import com.fullaccel.paircarserver.trip.domain.Boarding
import com.fullaccel.paircarserver.trip.domain.FuelEconomySource
import com.fullaccel.paircarserver.trip.domain.Trip
import com.fullaccel.paircarserver.trip.domain.TripSegment
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class TripResult(
    val tripId: Long,
    val crewId: Long,
    val driverCrewMemberId: Long,
    val driverMemberId: Long,
    val driverNickname: String,
    val carId: Long,
    val origin: PlaceSummary,
    val destination: PlaceSummary,
    // 경유지 (출발지, 목적지를 뺀 탈 지점, 내릴 지점. 탑승 순서대로 중복 없이)
    val waypoints: List<PlaceSummary>,
    val serviceDate: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant?,
    val startOdometerKm: BigDecimal,
    val endOdometerKm: BigDecimal?,
    val tollFareWon: Int,
    val boardings: List<BoardingResult>,
    val segments: List<SegmentResult>,
) {
    companion object {
        fun of(trip: Trip, boardings: List<Boarding>, segments: List<SegmentResult>): TripResult {
            val endpoints = setOf(trip.originPlace.id, trip.destinationPlace.id)
            val waypoints = boardings
                .flatMap { listOf(it.boardPlace, it.alightPlace) }
                .filter { it.id !in endpoints }
                .distinctBy { it.id }
                .map(PlaceSummary::from)
            return TripResult(
                tripId = trip.id,
                crewId = trip.driver.crew.id,
                driverCrewMemberId = trip.driver.id,
                driverMemberId = trip.driver.member.id,
                driverNickname = trip.driver.member.nickname,
                carId = trip.car.id,
                origin = PlaceSummary.from(trip.originPlace),
                destination = PlaceSummary.from(trip.destinationPlace),
                waypoints = waypoints,
                serviceDate = trip.serviceDate,
                startedAt = trip.startedAt,
                endedAt = trip.endedAt,
                startOdometerKm = trip.startOdometerKm,
                endOdometerKm = trip.endOdometerKm,
                tollFareWon = trip.tollFareWon,
                boardings = boardings.map(BoardingResult::from),
                segments = segments,
            )
        }
    }
}

data class PlaceSummary(
    val placeId: Long,
    val name: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
) {
    companion object {
        fun from(place: CrewPlace) = PlaceSummary(place.id, place.name, place.latitude, place.longitude)
    }
}

data class BoardingResult(
    val boardingId: Long,
    val crewMemberId: Long,
    val memberId: Long,
    val nickname: String,
    val boardPlaceId: Long,
    val alightPlaceId: Long,
    val boardedAt: Instant?,
    val boardOdometerKm: BigDecimal?,
    val alightedAt: Instant?,
    val alightOdometerKm: BigDecimal?,
) {
    companion object {
        fun from(boarding: Boarding) = BoardingResult(
            boardingId = boarding.id,
            crewMemberId = boarding.crewMember.id,
            memberId = boarding.crewMember.member.id,
            nickname = boarding.crewMember.member.nickname,
            boardPlaceId = boarding.boardPlace.id,
            alightPlaceId = boarding.alightPlace.id,
            boardedAt = boarding.boardedAt,
            boardOdometerKm = boarding.boardOdometerKm,
            alightedAt = boarding.alightedAt,
            alightOdometerKm = boarding.alightOdometerKm,
        )
    }
}

data class SegmentResult(
    val seq: Int,
    val startOdometerKm: BigDecimal,
    val endOdometerKm: BigDecimal,
    val fuelEconomy: BigDecimal,
    val fuelEconomySource: FuelEconomySource,
    val riderCrewMemberIds: List<Long>,
    val startPointIndex: Int?,
    val endPointIndex: Int?,
) {
    companion object {
        fun of(segment: TripSegment, riderCrewMemberIds: List<Long>) = SegmentResult(
            seq = segment.seq.toInt(),
            startOdometerKm = segment.startOdometerKm,
            endOdometerKm = segment.endOdometerKm,
            fuelEconomy = segment.fuelEconomy,
            fuelEconomySource = segment.fuelEconomySource,
            riderCrewMemberIds = riderCrewMemberIds,
            startPointIndex = segment.startPointIndex,
            endPointIndex = segment.endPointIndex,
        )
    }
}
