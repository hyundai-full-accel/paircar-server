package com.fullaccel.paircarserver.trip.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface BoardingRepository : JpaRepository<Boarding, Long> {

    @EntityGraph(attributePaths = ["crewMember.member", "boardPlace", "alightPlace"])
    fun findAllByTripIdOrderById(tripId: Long): List<Boarding>

    fun findByTripIdAndCrewMemberId(tripId: Long, crewMemberId: Long): Boarding?

    fun findAllByTripIdInAndBoardedAtIsNotNull(tripIds: Collection<Long>): List<Boarding>
}
