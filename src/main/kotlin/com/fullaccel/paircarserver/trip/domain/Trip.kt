package com.fullaccel.paircarserver.trip.domain

import com.fullaccel.paircarserver.car.domain.Car
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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// 크루 멤버가 멤버의 차로 출발지에서 목적지까지 운행함 (출발, 도착 측정값 포함)
@Entity
@Table(
    name = "trips",
    indexes = [Index(name = "idx_trips_driver_service_date", columnList = "driver_id, service_date")],
)
class Trip(
    // 운전한 크루 멤버 (크루는 driver.crew로 찾음)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false, updatable = false)
    val driver: CrewMember,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false, updatable = false)
    val car: Car,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_place_id", nullable = false, updatable = false)
    val originPlace: CrewPlace,

    // 내비 목적지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_place_id", nullable = false, updatable = false)
    val destinationPlace: CrewPlace,

    // 운행일 (KST 기준. 월 정산 집계 기준)
    @Column(nullable = false, updatable = false)
    val serviceDate: LocalDate,

    // 출발 시각 (시동)
    @Column(nullable = false, updatable = false)
    val startedAt: Instant,

    @Column(nullable = false, precision = 9, scale = 1, updatable = false)
    val startOdometerKm: BigDecimal,

    // 비어 있으면 운행 중
    @Column
    var endedAt: Instant? = null,

    @Column(precision = 9, scale = 1)
    var endOdometerKm: BigDecimal? = null,

    // 그날 통행료 (RouteStartInfo.tollFare)
    @Column(nullable = false)
    var tollFareWon: Int = 0,

    // 운행 전체 실제 주행 경로 (Encoded Polyline). 차량 앱이 FusedLocation으로 모아 도착 후 업로드
    @Column(columnDefinition = "text")
    var routePolyline: String? = null,
) : BaseEntity() {

    val isEnded: Boolean
        get() = endedAt != null

    fun end(endedAt: Instant, endOdometerKm: BigDecimal, tollFareWon: Int, routePolyline: String?) {
        this.endedAt = endedAt
        this.endOdometerKm = endOdometerKm
        this.tollFareWon = tollFareWon
        this.routePolyline = routePolyline
    }
}
