package com.fullaccel.paircarserver.settlement.application.dto

import com.fullaccel.paircarserver.crew.domain.SettlementMode
import com.fullaccel.paircarserver.settlement.domain.Receipt
import com.fullaccel.paircarserver.settlement.domain.ReceiptItem
import com.fullaccel.paircarserver.trip.domain.FuelEconomySource
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

// 월 영수증 (기획서 영수증 화면의 항목). 합계는 하루치 줄을 합산해 계산함
data class ReceiptResult(
    val receiptId: Long,
    val crewId: Long,
    val payerCrewMemberId: Long,
    val payerMemberId: Long,
    val payerNickname: String,
    val payeeCrewMemberId: Long,
    val payeeMemberId: Long,
    val payeeNickname: String,
    val month: YearMonth,
    val settlementMode: SettlementMode,
    val boardingDays: Int,             // 탑승일
    val sharedDistanceKm: BigDecimal,  // 함께 탄 거리
    val detourKm: BigDecimal,          // 우회 거리 합계
    @field:Schema(
        description = "내 부담 거리 (km) = 유류비 ÷ km당 비용. 함께 탄 구간은 구간 인원으로 나눈 몫이고, 우회 거리는 전부 포함. " +
            "기획서 영수증의 '내 몫 거리'",
        example = "162.0",
    )
    val ownDistanceKm: BigDecimal,
    val fuelEconomy: BigDecimal,       // 평균 연비
    val fuelEconomySource: FuelEconomySource,
    val fuelPriceWon: Int,             // 평균 유가
    val fuelCostWon: BigDecimal,       // 유류비 (올림 전)
    val tollWon: BigDecimal,           // 통행료
    val roundingWon: BigDecimal,       // 100원 단위 올림으로 더해진 금액
    val amountWon: Int,                // 보낼 금액
    val payeeBankName: String,
    val payeeAccountNumber: String,
    val payeeAccountHolder: String,
    val waivedAt: Instant?,
    val issuedAt: Instant,
    val items: List<ReceiptItemResult>,
) {
    companion object {
        fun of(receipt: Receipt, items: List<ReceiptItem>): ReceiptResult {
            val itemResults = items.map(ReceiptItemResult::from)
            val fuelCost = itemResults.fold(BigDecimal.ZERO) { sum, item -> sum + item.sharedCostWon + item.detourCostWon }
            val toll = itemResults.fold(BigDecimal.ZERO) { sum, item -> sum + item.tollWon }
            val ownDistance = if (receipt.fuelPriceWon == 0) {
                BigDecimal.ZERO
            } else {
                (fuelCost * receipt.fuelEconomy).divide(receipt.fuelPriceWon.toBigDecimal(), 1, RoundingMode.HALF_UP)
            }
            return ReceiptResult(
                receiptId = receipt.id,
                crewId = receipt.payer.crew.id,
                payerCrewMemberId = receipt.payer.id,
                payerMemberId = receipt.payer.member.id,
                payerNickname = receipt.payer.member.nickname,
                payeeCrewMemberId = receipt.payee.id,
                payeeMemberId = receipt.payee.member.id,
                payeeNickname = receipt.payee.member.nickname,
                month = YearMonth.from(receipt.month),
                settlementMode = receipt.settlementMode,
                boardingDays = itemResults.map { it.serviceDate }.toSet().size,
                sharedDistanceKm = itemResults.fold(BigDecimal.ZERO) { sum, item -> sum + item.sharedDistanceKm },
                detourKm = itemResults.fold(BigDecimal.ZERO) { sum, item -> sum + item.detourKm },
                ownDistanceKm = ownDistance,
                fuelEconomy = receipt.fuelEconomy,
                fuelEconomySource = receipt.fuelEconomySource,
                fuelPriceWon = receipt.fuelPriceWon,
                fuelCostWon = fuelCost,
                tollWon = toll,
                roundingWon = receipt.amountWon.toBigDecimal() - fuelCost - toll,
                amountWon = receipt.amountWon,
                payeeBankName = receipt.payeeBankName,
                payeeAccountNumber = receipt.payeeAccountNumber,
                payeeAccountHolder = receipt.payeeAccountHolder,
                waivedAt = receipt.waivedAt,
                issuedAt = receipt.issuedAt,
                items = itemResults,
            )
        }
    }
}

// 영수증 하루치 줄 (탑승 1건)
data class ReceiptItemResult(
    val boardingId: Long,
    val serviceDate: LocalDate,
    val sharedDistanceKm: BigDecimal,
    val sharedCostWon: BigDecimal,
    val detourKm: BigDecimal,
    val detourCostWon: BigDecimal,
    val tollWon: BigDecimal,
    val amountWon: BigDecimal,
) {
    companion object {
        fun from(item: ReceiptItem) = ReceiptItemResult(
            boardingId = item.boarding.id,
            serviceDate = item.boarding.trip.serviceDate,
            sharedDistanceKm = item.sharedDistanceKm,
            sharedCostWon = item.sharedCostWon,
            detourKm = item.detourKm,
            detourCostWon = item.detourCostWon,
            tollWon = item.tollWon,
            amountWon = item.sharedCostWon + item.detourCostWon + item.tollWon,
        )
    }
}
