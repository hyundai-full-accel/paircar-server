package com.fullaccel.paircarserver.crew.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface CrewAttendanceRepository : JpaRepository<CrewAttendance, Long> {

    fun findByCrewMemberIdAndServiceDate(crewMemberId: Long, serviceDate: LocalDate): CrewAttendance?

    @EntityGraph(attributePaths = ["boardPlace"])
    fun findAllByCrewMemberCrewIdAndServiceDate(crewId: Long, serviceDate: LocalDate): List<CrewAttendance>
}
