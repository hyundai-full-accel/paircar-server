package com.fullaccel.paircarserver.trip.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

// 구간에 탄 동승자 (구간과 탑승의 다대다 관계). 운전자는 넣지 않음
// 구간 탑승 인원 = 이 행 수 (+ crew.includeDriver면 운전자 1명)
@Entity
@Table(
    name = "trip_segment_riders",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_trip_segment_riders_segment_boarding", columnNames = ["trip_segment_id", "boarding_id"]),
    ],
    indexes = [Index(name = "idx_trip_segment_riders_boarding_id", columnList = "boarding_id")],
)
class TripSegmentRider(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_segment_id", nullable = false, updatable = false)
    val tripSegment: TripSegment,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boarding_id", nullable = false, updatable = false)
    val boarding: Boarding,
) : BaseEntity()
