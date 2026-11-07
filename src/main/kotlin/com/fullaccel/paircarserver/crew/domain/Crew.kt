package com.fullaccel.paircarserver.crew.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

// 카풀 크루
@Entity
@Table(name = "crews")
class Crew(
    // 크루 이름 (예: 판교 출근 크루)
    @Column(nullable = false, length = 50)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var settlementMode: SettlementMode = SettlementMode.FAIR,

    // 구간 탑승 인원에 운전자를 포함할지
    @Column(nullable = false)
    var includeDriver: Boolean = true,

    // 월 정액 금액 (settlementMode가 FIXED_MONTHLY일 때만)
    @Column
    var monthlyFixedWon: Int? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity() {

    // 아래는 크루 기본 일정. 운행은 그날 정한 출발지, 목적지로 시작하므로 바뀌어도 됨

    // 평소 출발 시각 (KST)
    @Column
    var departureTime: LocalTime? = null
        protected set

    // 카풀하는 요일
    @Convert(converter = DaysOfWeekConverter::class)
    @Column(nullable = false)
    var departureDays: Set<DayOfWeek> = emptySet()
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_place_id")
    var originPlace: CrewPlace? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_place_id")
    var destinationPlace: CrewPlace? = null
        protected set

    fun update(
        name: String,
        departureTime: LocalTime?,
        departureDays: Set<DayOfWeek>,
        originPlace: CrewPlace?,
        destinationPlace: CrewPlace?,
        settlementMode: SettlementMode,
        includeDriver: Boolean,
    ) {
        this.name = name
        this.departureTime = departureTime
        this.departureDays = departureDays
        this.originPlace = originPlace
        this.destinationPlace = destinationPlace
        this.settlementMode = settlementMode
        this.includeDriver = includeDriver
    }
}
