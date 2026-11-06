package com.fullaccel.paircarserver.trip.domain

import com.fullaccel.paircarserver.crew.domain.CrewMember
import com.fullaccel.paircarserver.crew.domain.CrewPlace
import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant

// 크루 멤버가 운행에 탑승함 (탑승, 하차 지점과 측정값 포함)
// 운행 시작 때 탈 사람마다 1행 생성. boardedAt이 끝까지 비어 있으면 미탑승 (정산 제외)
@Entity
@Table(
    name = "boardings",
    uniqueConstraints = [UniqueConstraint(name = "uk_boardings_trip_crew_member", columnNames = ["trip_id", "crew_member_id"])],
    indexes = [Index(name = "idx_boardings_crew_member_id", columnList = "crew_member_id")],
)
class Boarding(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false, updatable = false)
    val trip: Trip,

    // 탑승한 크루 멤버 (운전자 제외)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_member_id", nullable = false, updatable = false)
    val crewMember: CrewMember,

    // 탈 예정 지점 (경유지 설정에 사용). "오늘은 후문에서 탈게요"면 1회 변경
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_place_id", nullable = false)
    var boardPlace: CrewPlace,

    // 내릴 예정 지점 (실제 하차 위치와 다를 수 있음)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alight_place_id", nullable = false)
    var alightPlace: CrewPlace,

    // 탑승 감지 시 채움
    @Column
    var boardedAt: Instant? = null,

    // 탑승 시점 누적 주행거리 (구간 경계)
    @Column(precision = 9, scale = 1)
    var boardOdometerKm: BigDecimal? = null,

    @Column
    var alightedAt: Instant? = null,

    // 하차 시점 누적 주행거리 (구간 경계). 감지 실패 시 trip.endOdometerKm로 채움
    @Column(precision = 9, scale = 1)
    var alightOdometerKm: BigDecimal? = null,
) : BaseEntity()
