package com.fullaccel.paircarserver.car.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface MemberCarRepository : JpaRepository<MemberCar, Long> {

    fun existsByMemberIdAndCarId(memberId: Long, carId: Long): Boolean

    fun existsByMemberId(memberId: Long): Boolean

    @EntityGraph(attributePaths = ["car"])
    fun findAllByMemberId(memberId: Long): List<MemberCar>
}
