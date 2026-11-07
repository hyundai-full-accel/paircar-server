package com.fullaccel.paircarserver.trip.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface TripRepository : JpaRepository<Trip, Long> {

    // 멤버가 어느 크루에서든 운전 중인, 아직 끝나지 않은 운행
    fun findFirstByDriverMemberIdAndEndedAtIsNull(memberId: Long): Trip?

    // 크루의 진행 중인 운행. 운전자가 여럿이면 가장 최근에 시작한 것
    fun findFirstByDriverCrewIdAndEndedAtIsNullOrderByStartedAtDesc(crewId: Long): Trip?

    // 운행 목록: 크루의 운행일 범위 안 운행 (시작 순)
    @EntityGraph(attributePaths = ["driver.member", "originPlace", "destinationPlace"])
    fun findAllByDriverCrewIdAndServiceDateBetweenOrderByStartedAtAsc(crewId: Long, from: LocalDate, to: LocalDate): List<Trip>

    // 정산 대상: 크루의 끝난 운행 중 운행일이 until 이전인 것
    @EntityGraph(attributePaths = ["car"])
    fun findAllByDriverCrewIdAndEndedAtIsNotNullAndServiceDateBeforeOrderByServiceDateAscIdAsc(
        crewId: Long,
        until: LocalDate,
    ): List<Trip>
}
