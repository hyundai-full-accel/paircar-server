package com.fullaccel.paircarserver.crew.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant

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

    // 직접 입력한 유가 (공공 유가 API를 못 쓸 때만. 비어 있으면 fuel_prices 사용)
    @Column
    var manualFuelPriceWon: Int? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity()
