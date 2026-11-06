package com.fullaccel.paircarserver.trip.domain

import org.springframework.data.jpa.repository.JpaRepository

interface TripSegmentRiderRepository : JpaRepository<TripSegmentRider, Long>
