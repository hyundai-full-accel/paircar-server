package com.fullaccel.paircarserver.crew.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CrewRepository : JpaRepository<Crew, Long> {

    @Query("select c.id from Crew c order by c.id")
    fun findAllIds(): List<Long>
}
