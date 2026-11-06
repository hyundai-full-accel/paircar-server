package com.fullaccel.paircarserver.crew.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface CrewInviteRepository : JpaRepository<CrewInvite, Long> {

    @EntityGraph(attributePaths = ["crew"])
    fun findByCode(code: String): CrewInvite?

    fun existsByCode(code: String): Boolean
}
