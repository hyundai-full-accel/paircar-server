package com.fullaccel.paircarserver.crew.domain

import org.springframework.data.jpa.repository.JpaRepository

interface CrewMemberRepository : JpaRepository<CrewMember, Long>
