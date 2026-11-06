package com.fullaccel.paircarserver.trip.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface TripSegmentRiderRepository : JpaRepository<TripSegmentRider, Long> {

    @EntityGraph(attributePaths = ["boarding"])
    fun findAllByTripSegmentTripId(tripId: Long): List<TripSegmentRider>
}
