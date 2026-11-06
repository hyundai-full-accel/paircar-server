package com.fullaccel.paircarserver.trip.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface TripRepository : JpaRepository<Trip, Long> {

    // 멤버가 어느 크루에서든 아직 끝나지 않은 운행을 운전 중인지
    fun existsByDriverMemberIdAndEndedAtIsNull(memberId: Long): Boolean

    // 정산 대상: 크루의 끝난 운행 중 운행일이 until 이전인 것
    @EntityGraph(attributePaths = ["car"])
    fun findAllByDriverCrewIdAndEndedAtIsNotNullAndServiceDateBeforeOrderByServiceDateAscIdAsc(
        crewId: Long,
        until: LocalDate,
    ): List<Trip>
}
