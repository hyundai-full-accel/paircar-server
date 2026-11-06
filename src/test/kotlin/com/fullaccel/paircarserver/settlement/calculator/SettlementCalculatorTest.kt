package com.fullaccel.paircarserver.settlement.calculator

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class SettlementCalculatorTest {

    private fun km(value: String) = BigDecimal(value)

    private fun assertWon(expected: String, actual: BigDecimal) = assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")

    // 기획서 예시: 함께 탄 구간 12km(운전자 포함 2인), 우회 거리 3km, 연비 13.2, 유가 1,684원, 18일 → 20,700원
    private val exampleRide = FairRideInput(
        segments = listOf(SegmentPortion(km("12.0"), km("13.20"), headCount = 2)),
        detourKm = km("3.0"),
        detourFuelEconomy = km("13.20"),
        fuelPriceWon = 1684,
        tollShareWon = BigDecimal.ZERO,
    )

    @Test
    fun `기획서 예시의 하루 부담금`() {
        val cost = SettlementCalculator.fairRide(exampleRide)

        assertWon("765.45", cost.sharedCostWon)
        assertWon("382.73", cost.detourCostWon)
        assertWon("1148.18", cost.amountWon)
    }

    @Test
    fun `기획서 예시의 월 청구액은 20,700원`() {
        val costs = List(18) { SettlementCalculator.fairRide(exampleRide) }

        assertEquals(20_700, SettlementCalculator.monthlyAmountWon(costs))
        assertWon("13.20", SettlementCalculator.averageFuelEconomy(costs))
        assertEquals(1684, SettlementCalculator.averageFuelPriceWon(costs))
    }

    @Test
    fun `동승자 2명이 겹쳐 탄 구간은 인원만큼 나눈다`() {
        // 수아: 정문~회사 12km 중 앞 4km는 수아+운전자 2인, 뒤 8km는 민준까지 3인
        val sua = SettlementCalculator.fairRide(
            FairRideInput(
                segments = listOf(
                    SegmentPortion(km("4.0"), km("10.00"), headCount = 2),
                    SegmentPortion(km("8.0"), km("10.00"), headCount = 3),
                ),
                detourKm = BigDecimal.ZERO,
                detourFuelEconomy = km("10.00"),
                fuelPriceWon = 1500,
                tollShareWon = BigDecimal.ZERO,
            ),
        )

        // km당 150원: 4×150÷2 + 8×150÷3 = 300 + 400
        assertWon("700.00", sua.sharedCostWon)
        assertWon("12.0", sua.sharedDistanceKm)
    }

    @Test
    fun `통행료 분담은 부담금에 더해진다`() {
        val cost = SettlementCalculator.fairRide(exampleRide.copy(tollShareWon = BigDecimal("600")))

        assertWon("1748.18", cost.amountWon)
    }

    @Test
    fun `100원 단위로 딱 떨어지면 그대로`() {
        val ride = FairRideInput(
            segments = listOf(SegmentPortion(km("10.0"), km("10.00"), headCount = 1)),
            detourKm = BigDecimal.ZERO,
            detourFuelEconomy = km("10.00"),
            fuelPriceWon = 2000,
            tollShareWon = BigDecimal.ZERO,
        )

        assertEquals(2_000, SettlementCalculator.monthlyAmountWon(listOf(SettlementCalculator.fairRide(ride))))
    }

    @Test
    fun `1원이라도 넘으면 다음 100원으로 올린다`() {
        val ride = FairRideInput(
            segments = listOf(SegmentPortion(km("10.0"), km("10.00"), headCount = 1)),
            detourKm = BigDecimal.ZERO,
            detourFuelEconomy = km("10.00"),
            fuelPriceWon = 2000,
            tollShareWon = BigDecimal("0.01"),
        )

        assertEquals(2_100, SettlementCalculator.monthlyAmountWon(listOf(SettlementCalculator.fairRide(ride))))
    }

    @Test
    fun `균등 분할은 운행 전체 비용을 인원으로 똑같이 나눈다`() {
        val cost = SettlementCalculator.equalRide(
            EqualRideInput(
                tripSegments = listOf(SegmentPortion(km("3.0"), km("10.00")), SegmentPortion(km("12.0"), km("10.00"))),
                headCount = 3,
                fuelPriceWon = 1500,
                tollShareWon = BigDecimal.ZERO,
            ),
        )

        // 15km × 150원 ÷ 3
        assertWon("750.00", cost.sharedCostWon)
        assertWon("0", cost.detourCostWon)
    }
}
