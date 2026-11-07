package com.fullaccel.paircarserver.trip.application.dto

// TRIP_ALREADY_IN_PROGRESS 실패 응답의 data. 이 운행을 이어서 끝내면 됨
data class TripInProgress(
    val tripId: Long,
    val crewId: Long,
)
