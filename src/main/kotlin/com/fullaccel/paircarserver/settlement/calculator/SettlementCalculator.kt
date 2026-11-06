package com.fullaccel.paircarserver.settlement.calculator

import java.math.BigDecimal
import java.math.RoundingMode

// 정산 계산 엔진 (기술명세 T5). DB, Spring에 의존하지 않는 순수 계산
// km당 비용 cd = 유가 ÷ 연비
// 공정 분할: 탑승 1건 부담금 = Σ(구간 거리 × cd ÷ 구간 인원) + 우회 거리 × cd + 통행료 분담
// 균등 분할: 탑승 1건 부담금 = Σ(운행 전체 구간 거리 × cd) ÷ 운행 인원 + 통행료 분담
// 월 청구액 = ⌈Σ 탑승 부담금 ÷ 100⌉ × 100
object SettlementCalculator {

    private const val CALC_SCALE = 10
    private const val WON_SCALE = 2
    private val HUNDRED = BigDecimal(100)

    fun costPerKm(fuelPriceWon: Int, fuelEconomy: BigDecimal): BigDecimal =
        BigDecimal(fuelPriceWon).divide(fuelEconomy, CALC_SCALE, RoundingMode.HALF_UP)

    fun fairRide(input: FairRideInput): RideCost {
        val sharedCost = input.segments.fold(BigDecimal.ZERO) { sum, segment ->
            sum + segment.distanceKm * costPerKm(input.fuelPriceWon, segment.fuelEconomy) / segment.headCount.toBigDecimal()
        }
        val detourCost = input.detourKm * costPerKm(input.fuelPriceWon, input.detourFuelEconomy)
        val fuelUsed = input.segments.fold(BigDecimal.ZERO) { sum, segment -> sum + fuel(segment.distanceKm, segment.fuelEconomy) } +
            fuel(input.detourKm, input.detourFuelEconomy)
        return RideCost(
            sharedDistanceKm = input.segments.fold(BigDecimal.ZERO) { sum, segment -> sum + segment.distanceKm },
            sharedCostWon = sharedCost.toWon(),
            detourKm = input.detourKm,
            detourCostWon = detourCost.toWon(),
            tollWon = input.tollShareWon.toWon(),
            fuelUsed = fuelUsed,
            fuelPriceWon = input.fuelPriceWon,
        )
    }

    fun equalRide(input: EqualRideInput): RideCost {
        val tripCost = input.tripSegments.fold(BigDecimal.ZERO) { sum, segment ->
            sum + segment.distanceKm * costPerKm(input.fuelPriceWon, segment.fuelEconomy)
        }
        val headCount = input.headCount.toBigDecimal()
        return RideCost(
            sharedDistanceKm = input.tripSegments.fold(BigDecimal.ZERO) { sum, segment -> sum + segment.distanceKm },
            sharedCostWon = (tripCost / headCount).toWon(),
            detourKm = BigDecimal.ZERO,
            detourCostWon = BigDecimal.ZERO.toWon(),
            tollWon = input.tollShareWon.toWon(),
            fuelUsed = input.tripSegments.fold(BigDecimal.ZERO) { sum, segment -> sum + fuel(segment.distanceKm, segment.fuelEconomy) },
            fuelPriceWon = input.fuelPriceWon,
        )
    }

    // 100원 단위 올림
    fun monthlyAmountWon(costs: List<RideCost>): Int {
        val total = costs.fold(BigDecimal.ZERO) { sum, cost -> sum + cost.amountWon }
        return total.divide(HUNDRED, 0, RoundingMode.CEILING).multiply(HUNDRED).intValueExact()
    }

    // 영수증 표시용 평균 연비 = 전체 거리 ÷ 전체 연료 사용량
    fun averageFuelEconomy(costs: List<RideCost>): BigDecimal {
        val fuelUsed = costs.fold(BigDecimal.ZERO) { sum, cost -> sum + cost.fuelUsed }
        if (fuelUsed.signum() == 0) return BigDecimal.ZERO.setScale(2)
        val distance = costs.fold(BigDecimal.ZERO) { sum, cost -> sum + cost.sharedDistanceKm + cost.detourKm }
        return distance.divide(fuelUsed, 2, RoundingMode.HALF_UP)
    }

    // 영수증 표시용 유가 = 연료 사용량 가중 평균 (유가가 하나면 그 값)
    fun averageFuelPriceWon(costs: List<RideCost>): Int {
        val fuelUsed = costs.fold(BigDecimal.ZERO) { sum, cost -> sum + cost.fuelUsed }
        if (fuelUsed.signum() == 0) return costs.firstOrNull()?.fuelPriceWon ?: 0
        val weighted = costs.fold(BigDecimal.ZERO) { sum, cost -> sum + cost.fuelUsed * cost.fuelPriceWon.toBigDecimal() }
        return weighted.divide(fuelUsed, 0, RoundingMode.HALF_UP).intValueExact()
    }

    private fun fuel(distanceKm: BigDecimal, fuelEconomy: BigDecimal): BigDecimal =
        if (distanceKm.signum() == 0) BigDecimal.ZERO else distanceKm.divide(fuelEconomy, CALC_SCALE, RoundingMode.HALF_UP)

    private operator fun BigDecimal.div(other: BigDecimal): BigDecimal = divide(other, CALC_SCALE, RoundingMode.HALF_UP)

    private fun BigDecimal.toWon(): BigDecimal = setScale(WON_SCALE, RoundingMode.HALF_UP)
}

// 동승자가 탄 구간 하나. headCount는 운전자 포함 여부를 이미 반영한 구간 인원
data class SegmentPortion(
    val distanceKm: BigDecimal,
    val fuelEconomy: BigDecimal,
    val headCount: Int = 1,
)

data class FairRideInput(
    val segments: List<SegmentPortion>,
    val detourKm: BigDecimal,
    val detourFuelEconomy: BigDecimal, // 우회 거리에 적용할 연비 (운행 평균 연비)
    val fuelPriceWon: Int,
    val tollShareWon: BigDecimal,
)

data class EqualRideInput(
    val tripSegments: List<SegmentPortion>, // 운행 전체 구간 (headCount는 쓰지 않음)
    val headCount: Int,                     // 운행 인원 (운전자 포함 여부 반영)
    val fuelPriceWon: Int,
    val tollShareWon: BigDecimal,
)

// 탑승 1건의 계산 결과 (영수증 하루치 줄)
data class RideCost(
    val sharedDistanceKm: BigDecimal,
    val sharedCostWon: BigDecimal,
    val detourKm: BigDecimal,
    val detourCostWon: BigDecimal,
    val tollWon: BigDecimal,
    val fuelUsed: BigDecimal, // 평균 연비, 평균 유가 표시용 연료 사용량 (L, EV는 kWh)
    val fuelPriceWon: Int,
) {
    val amountWon: BigDecimal
        get() = sharedCostWon + detourCostWon + tollWon
}
