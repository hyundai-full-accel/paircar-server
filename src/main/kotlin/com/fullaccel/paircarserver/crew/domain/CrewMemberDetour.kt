package com.fullaccel.paircarserver.crew.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant

// 운전자가 동승자를 이 장소에서 태우거나 내려 주느라 더 달리는 거리 (기술명세 T4)
// 크루 합류 때 1번 산출하고 양측이 확인한 값을 정산 기준으로 씀. 매일 다시 계산하지 않음
@Entity
@Table(
    name = "crew_member_detours",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_crew_member_detours_passenger_driver_place",
            columnNames = ["passenger_id", "driver_id", "place_id"],
        ),
    ],
)
class CrewMemberDetour(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false, updatable = false)
    val passenger: CrewMember,

    // 교대 운전이면 운전자마다 따로
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false, updatable = false)
    val driver: CrewMember,

    // 경유하는 장소 (승차 지점 또는 하차 지점)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false, updatable = false)
    val place: CrewPlace,

    // 우회 거리 (경유 거리 - 직행 거리)
    @Column(nullable = false, precision = 6, scale = 1)
    var detourKm: BigDecimal,

    @Column
    var driverConfirmedAt: Instant? = null,

    // 운전자, 동승자 확인 시각이 둘 다 채워져야 정산에 사용
    @Column
    var passengerConfirmedAt: Instant? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    // Odometer 실측 보정에 크루가 동의했을 때 갱신
    @Column(nullable = false)
    var updatedAt: Instant = createdAt,
) : BaseEntity() {

    // 값이 바뀌면 양측 확인을 다시 받아야 함
    fun changeDetourKm(detourKm: BigDecimal) {
        if (this.detourKm.compareTo(detourKm) == 0) return
        this.detourKm = detourKm
        this.driverConfirmedAt = null
        this.passengerConfirmedAt = null
        this.updatedAt = Instant.now()
    }

    fun confirmBy(crewMemberId: Long): Boolean {
        val now = Instant.now()
        when (crewMemberId) {
            driver.id -> driverConfirmedAt = now
            passenger.id -> passengerConfirmedAt = now
            else -> return false
        }
        return true
    }

    // 운전자, 동승자 둘 다 확인해야 정산에 사용
    val isConfirmed: Boolean
        get() = driverConfirmedAt != null && passengerConfirmedAt != null
}
