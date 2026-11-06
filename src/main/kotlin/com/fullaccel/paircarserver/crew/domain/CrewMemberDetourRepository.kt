package com.fullaccel.paircarserver.crew.domain

import org.springframework.data.jpa.repository.JpaRepository

interface CrewMemberDetourRepository : JpaRepository<CrewMemberDetour, Long> {

    fun findByPassengerIdAndDriverIdAndPlaceId(passengerId: Long, driverId: Long, placeId: Long): CrewMemberDetour?

    fun findAllByPassengerCrewIdOrderById(crewId: Long): List<CrewMemberDetour>
}
