package com.fullaccel.paircarserver.car.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MemberCarRepository : JpaRepository<MemberCar, Long> {

    fun existsByMemberIdAndCarId(memberId: Long, carId: Long): Boolean

    fun existsByMemberId(memberId: Long): Boolean

    @Query("select distinct mc.member.id from MemberCar mc where mc.member.id in :memberIds")
    fun findMemberIdsWithCar(memberIds: Collection<Long>): List<Long>

    @EntityGraph(attributePaths = ["car"])
    fun findAllByMemberId(memberId: Long): List<MemberCar>
}
