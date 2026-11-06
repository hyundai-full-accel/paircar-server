package com.fullaccel.paircarserver.crew.domain

import org.springframework.data.jpa.repository.JpaRepository

interface CrewPlaceRepository : JpaRepository<CrewPlace, Long> {

    fun existsByCrewIdAndName(crewId: Long, name: String): Boolean

    fun findAllByCrewIdOrderById(crewId: Long): List<CrewPlace>
}
