package com.fullaccel.paircarserver.settlement.domain

import com.fullaccel.paircarserver.car.domain.FuelType
import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.time.LocalDate

// 월평균 유가 (공공 유가 정보 API에서 수집)
@Entity
@Table(
    name = "fuel_prices",
    uniqueConstraints = [UniqueConstraint(name = "uk_fuel_prices_fuel_type_month", columnNames = ["fuel_type", "month"])],
)
class FuelPrice(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    val fuelType: FuelType,

    // 기준 월 (매월 1일. 예: 2026-11-01)
    @Column(nullable = false, updatable = false)
    val month: LocalDate,

    // 월평균 가격 (원/L, EV는 원/kWh)
    @Column(nullable = false)
    var priceWon: Int,

    @Column(nullable = false)
    var collectedAt: Instant = Instant.now(),
) : BaseEntity()
