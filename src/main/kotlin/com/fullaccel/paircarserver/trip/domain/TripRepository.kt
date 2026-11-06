package com.fullaccel.paircarserver.trip.domain

import org.springframework.data.jpa.repository.JpaRepository

interface TripRepository : JpaRepository<Trip, Long> {

    // 멤버가 어느 크루에서든 아직 끝나지 않은 운행을 운전 중인지
    fun existsByDriverMemberIdAndEndedAtIsNull(memberId: Long): Boolean
}
