package com.fullaccel.paircarserver.settlement.domain

import com.fullaccel.paircarserver.crew.domain.CrewMember
import com.fullaccel.paircarserver.crew.domain.SettlementMode
import com.fullaccel.paircarserver.global.entity.BaseEntity
import com.fullaccel.paircarserver.trip.domain.FuelEconomySource
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// 동승자가 운전자에게 낼 월 영수증 (월말 1번 발행. 발행 후 고정)
// 탑승 일수, 거리, 유류비, 통행료 합계는 ReceiptItem을 합산해 구함 (저장하지 않음)
@Entity
@Table(
    name = "receipts",
    uniqueConstraints = [UniqueConstraint(name = "uk_receipts_payer_payee_month", columnNames = ["payer_id", "payee_id", "month"])],
    indexes = [Index(name = "idx_receipts_payee_month", columnList = "payee_id, month")],
)
class Receipt(
    // 내는 사람 (동승자)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id", nullable = false, updatable = false)
    val payer: CrewMember,

    // 받는 사람 (운전자)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payee_id", nullable = false, updatable = false)
    val payee: CrewMember,

    // 정산 월 (매월 1일. 예: 2026-11-01)
    @Column(nullable = false, updatable = false)
    val month: LocalDate,

    // 발행 시점 크루 정산 방식 스냅샷
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    val settlementMode: SettlementMode,

    // 평균 연비 (거리 가중 평균. 영수증 표시값)
    @Column(nullable = false, precision = 5, scale = 2, updatable = false)
    val fuelEconomy: BigDecimal,

    // 입력 연비가 하나라도 섞이면 MANUAL
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    val fuelEconomySource: FuelEconomySource,

    // 적용 유가 (원/L, EV는 원/kWh)
    @Column(nullable = false, updatable = false)
    val fuelPriceWon: Int,

    // 보낼 금액 (ReceiptItem 합계를 100원 단위 올림. 월 정액이면 정액)
    @Column(nullable = false, updatable = false)
    val amountWon: Int,

    // 발행 시점 계좌 스냅샷
    @Column(nullable = false, length = 30, updatable = false)
    val payeeBankName: String,

    @Column(nullable = false, length = 30, updatable = false)
    val payeeAccountNumber: String,

    @Column(nullable = false, length = 30, updatable = false)
    val payeeAccountHolder: String,

    // 면제 시각 ("이번 달은 내가 낼게"). 채워지면 0원 영수증
    @Column
    var waivedAt: Instant? = null,

    @Column(nullable = false, updatable = false)
    val issuedAt: Instant = Instant.now(),
) : BaseEntity()
