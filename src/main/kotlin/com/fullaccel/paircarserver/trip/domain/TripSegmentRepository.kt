package com.fullaccel.paircarserver.trip.domain

import org.springframework.data.jpa.repository.JpaRepository

interface TripSegmentRepository : JpaRepository<TripSegment, Long> {

    fun findAllByTripIdOrderBySeq(tripId: Long): List<TripSegment>

    fun findAllByTripIdIn(tripIds: Collection<Long>): List<TripSegment>
}
