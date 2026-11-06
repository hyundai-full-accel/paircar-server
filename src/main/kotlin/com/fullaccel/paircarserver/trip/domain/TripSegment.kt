package com.fullaccel.paircarserver.trip.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal

// 운행을 탑승 인원이 바뀌는 지점마다 나눈 구간 (기술명세 T5)
// 출발부터 도착까지 모든 구간을 저장. 운전자 혼자 달린 구간(TripSegmentRider 없음)은 정산하지 않음
@Entity
@Table(
    name = "trip_segments",
    uniqueConstraints = [UniqueConstraint(name = "uk_trip_segments_trip_seq", columnNames = ["trip_id", "seq"])],
)
class TripSegment(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false, updatable = false)
    val trip: Trip,

    // 운행 안에서 구간 순서 (1부터)
    @Column(nullable = false, updatable = false)
    val seq: Short,

    @Column(nullable = false, precision = 9, scale = 1, updatable = false)
    val startOdometerKm: BigDecimal,

    @Column(nullable = false, precision = 9, scale = 1, updatable = false)
    val endOdometerKm: BigDecimal,

    // 구간 평균 연비 (km/L, EV는 km/kWh)
    @Column(nullable = false, precision = 5, scale = 2, updatable = false)
    val fuelEconomy: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    val fuelEconomySource: FuelEconomySource,

    // trip.routePolyline에서 이 구간이 시작, 끝나는 점 번호 (구간 경로 = 전체 경로를 이 범위로 자른 것)
    @Column
    var startPointIndex: Int? = null,

    @Column
    var endPointIndex: Int? = null,
) : BaseEntity()
