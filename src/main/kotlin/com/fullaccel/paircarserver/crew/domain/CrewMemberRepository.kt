package com.fullaccel.paircarserver.crew.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface CrewMemberRepository : JpaRepository<CrewMember, Long> {

    fun existsByCrewIdAndMemberId(crewId: Long, memberId: Long): Boolean

    fun findByCrewIdAndMemberId(crewId: Long, memberId: Long): CrewMember?

    @EntityGraph(attributePaths = ["member"])
    fun findAllByCrewIdOrderById(crewId: Long): List<CrewMember>
}
