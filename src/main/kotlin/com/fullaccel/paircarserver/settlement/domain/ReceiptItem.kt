package com.fullaccel.paircarserver.settlement.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import com.fullaccel.paircarserver.trip.domain.Boarding
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.math.BigDecimal

// 월 영수증의 하루치 줄 (탑승 1건 = 1줄)
// 이날 부담금 = sharedCostWon + detourCostWon + tollWon
@Entity
@Table(
    name = "receipt_items",
    indexes = [Index(name = "idx_receipt_items_receipt_id", columnList = "receipt_id")],
)
class ReceiptItem(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false, updatable = false)
    val receipt: Receipt,

    // 정산한 탑승 (탑승 1건은 영수증 1장에만). 이 행이 없는 탑승은 미정산
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boarding_id", nullable = false, unique = true, updatable = false)
    val boarding: Boarding,

    // 이날 함께 탄 거리
    @Column(nullable = false, precision = 8, scale = 1, updatable = false)
    val sharedDistanceKm: BigDecimal,

    // 이날 구간 분담금 합계 (Σ 구간 거리 × km당 비용 ÷ 구간 인원)
    @Column(nullable = false, precision = 10, scale = 2, updatable = false)
    val sharedCostWon: BigDecimal,

    // 이날 적용한 우회 거리 (CrewMemberDetour 스냅샷)
    @Column(nullable = false, precision = 6, scale = 1, updatable = false)
    val detourKm: BigDecimal,

    // 우회 거리 × km당 비용
    @Column(nullable = false, precision = 10, scale = 2, updatable = false)
    val detourCostWon: BigDecimal,

    // 이날 통행료 분담
    @Column(nullable = false, precision = 10, scale = 2, updatable = false)
    val tollWon: BigDecimal,
) : BaseEntity()
