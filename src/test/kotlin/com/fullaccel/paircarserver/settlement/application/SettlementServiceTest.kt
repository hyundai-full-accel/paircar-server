package com.fullaccel.paircarserver.settlement.application

import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.car.application.CarService
import com.fullaccel.paircarserver.car.application.dto.RegisterCarCommand
import com.fullaccel.paircarserver.car.application.dto.UpdateCarCommand
import com.fullaccel.paircarserver.car.domain.FuelType
import com.fullaccel.paircarserver.crew.application.CrewDetourService
import com.fullaccel.paircarserver.crew.application.CrewPlaceService
import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.crew.application.dto.RegisterDetourCommand
import com.fullaccel.paircarserver.crew.application.dto.RegisterPlaceCommand
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import com.fullaccel.paircarserver.member.application.dto.BankAccountCommand
import com.fullaccel.paircarserver.settlement.application.event.ReceiptIssuedEvent
import com.fullaccel.paircarserver.trip.application.TripService
import com.fullaccel.paircarserver.trip.application.dto.BoardingEventCommand
import com.fullaccel.paircarserver.trip.application.dto.EndTripCommand
import com.fullaccel.paircarserver.trip.application.dto.RiderCommand
import com.fullaccel.paircarserver.trip.application.dto.SegmentCommand
import com.fullaccel.paircarserver.trip.application.dto.StartTripCommand
import com.fullaccel.paircarserver.trip.domain.FuelEconomySource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@IntegrationTest
@RecordApplicationEvents
class SettlementServiceTest(
    @Autowired private val settlementService: SettlementService,
    @Autowired private val tripService: TripService,
    @Autowired private val memberService: MemberService,
    @Autowired private val carService: CarService,
    @Autowired private val crewService: CrewService,
    @Autowired private val crewPlaceService: CrewPlaceService,
    @Autowired private val crewDetourService: CrewDetourService,
    @Autowired private val monthlySettlementScheduler: MonthlySettlementScheduler,
) {

    @Autowired
    private lateinit var events: ApplicationEvents

    private val november = YearMonth.of(2026, 11)
    private val december = YearMonth.of(2026, 12) // 테스트의 오늘(2027-01-15) 기준 지난달

    private var jihoon = 0L      // 운전자 멤버 ID
    private var driverId = 0L    // 운전자 크루 멤버 ID
    private var sua = 0L         // 동승자 크루 멤버 ID
    private var crewId = 0L
    private var carId = 0L
    private var home = 0L
    private var gate = 0L
    private var office = 0L
    private var odometer = BigDecimal("10000.0")

    @BeforeEach
    fun setUp() {
        jihoon = memberService.createMember("지훈").memberId
        memberService.registerBankAccount(jihoon, BankAccountCommand("○○은행", "3333-01-1234567", "지훈"))
        carId = carService.registerCar(jihoon, RegisterCarCommand("VK-1", FuelType.GASOLINE)).carId
        carService.updateCar(jihoon, carId, UpdateCarCommand(FuelType.GASOLINE, null, 1684))
        crewId = crewService.createCrew(jihoon, "판교 출근 크루").crewId
        driverId = crewService.getCrewMemberEntity(crewId, jihoon).id
        sua = crewService.addMember(crewId, memberService.createMember("수아").memberId).crewMemberId
        home = place("지훈 집")
        gate = place("○○아파트 정문")
        office = place("판교 본관")
    }

    private fun place(name: String) =
        crewPlaceService.registerPlace(crewId, RegisterPlaceCommand(name, BigDecimal("37.4"), BigDecimal("127.1"))).placeId

    private fun confirmDetour(km: String) {
        val detour = crewDetourService.registerDetour(crewId, RegisterDetourCommand(sua, driverId, gate, BigDecimal(km)))
        crewDetourService.confirmDetour(detour.detourId, driverId)
        crewDetourService.confirmDetour(detour.detourId, sua)
    }

    // 기획서 시나리오 하루: 집 → (혼자 5km) → 정문에서 수아 탑승 → (함께 12km) → 본관
    // 운전자 교체 시험은 driverMemberId, car, rider를 바꿔서 부름
    private fun commute(
        date: LocalDate,
        toll: Int = 0,
        driverMemberId: Long = jihoon,
        car: Long = carId,
        rider: Long = sua,
    ) {
        val startedAt = date.atTime(LocalTime.of(8, 0)).atZone(ZoneId.of("Asia/Seoul")).toInstant()
        val start = odometer
        val boardAt = start + BigDecimal("5.0")
        val end = boardAt + BigDecimal("12.0")
        val trip = tripService.startTrip(
            driverMemberId,
            StartTripCommand(crewId, car, home, office, startedAt, start, listOf(RiderCommand(rider, gate, office))),
        )
        tripService.recordBoarding(driverMemberId, trip.tripId, rider, BoardingEventCommand(startedAt.plusSeconds(600), boardAt))
        tripService.uploadSegments(
            driverMemberId,
            trip.tripId,
            listOf(
                SegmentCommand(1, start, boardAt, BigDecimal("13.20"), FuelEconomySource.MEASURED, emptyList()),
                SegmentCommand(2, boardAt, end, BigDecimal("13.20"), FuelEconomySource.MEASURED, listOf(rider)),
            ),
        )
        tripService.endTrip(driverMemberId, trip.tripId, EndTripCommand(startedAt.plusSeconds(1800), end, toll))
        odometer = end
    }

    private fun commuteDays(month: YearMonth, days: Int) =
        (1..month.lengthOfMonth()).map { month.atDay(it) }.take(days).forEach { commute(it) }

    private fun assertDecimal(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")

    @Test
    fun `기획서 예시 - 18일 탑승, 함께 탄 거리 216km, 우회 54km, 보낼 금액 20,700원`() {
        confirmDetour("3.0")
        commuteDays(november, 18)

        val receipt = settlementService.issueReceipts(crewId, november).single()

        assertEquals("수아", receipt.payerNickname)
        assertEquals("지훈", receipt.payeeNickname)
        assertEquals(18, receipt.boardingDays)
        assertDecimal("216.0", receipt.sharedDistanceKm)
        assertDecimal("54.0", receipt.detourKm)
        assertDecimal("162.0", receipt.ownDistanceKm)
        assertDecimal("13.20", receipt.fuelEconomy)
        assertEquals(1684, receipt.fuelPriceWon)
        assertDecimal("20667.24", receipt.fuelCostWon)
        assertEquals(20_700, receipt.amountWon)
        assertEquals("3333-01-1234567", receipt.payeeAccountNumber)
        assertEquals(18, receipt.items.size)
    }

    @Test
    fun `확인받지 않은 우회 거리는 정산에 쓰지 않는다`() {
        crewDetourService.registerDetour(crewId, RegisterDetourCommand(sua, driverId, gate, BigDecimal("3.0")))
        commuteDays(november, 1)

        val receipt = settlementService.issueReceipts(crewId, november).single()

        assertDecimal("0", receipt.detourKm)
        assertDecimal("765.45", receipt.fuelCostWon)
    }

    @Test
    fun `우회 거리는 탑승 기록의 탈 지점, 내릴 지점과 같은 장소일 때만 쓴다`() {
        // 차량 앱 연동에서 확인한 우회 거리가 0으로 나온 사례: 우회 거리를 등록한 장소와 운행 시작 때 보낸 탈 지점이 다름
        val backGate = place("○○아파트 후문")
        val detour = crewDetourService.registerDetour(crewId, RegisterDetourCommand(sua, driverId, backGate, BigDecimal("3.0")))
        crewDetourService.confirmDetour(detour.detourId, driverId)
        crewDetourService.confirmDetour(detour.detourId, sua)
        commuteDays(november, 1) // 정문에서 탑승

        val receipt = settlementService.issueReceipts(crewId, november).single()

        assertDecimal("0", receipt.detourKm)
    }

    @Test
    fun `출발지나 목적지에 등록한 우회 거리는 쓰지 않는다`() {
        // 출발지, 목적지는 원래 가는 길이라 돌아가는 거리가 아님
        val detour = crewDetourService.registerDetour(crewId, RegisterDetourCommand(sua, driverId, office, BigDecimal("3.0")))
        crewDetourService.confirmDetour(detour.detourId, driverId)
        crewDetourService.confirmDetour(detour.detourId, sua)
        commuteDays(november, 1)

        val receipt = settlementService.issueReceipts(crewId, november).single()

        assertDecimal("0", receipt.detourKm)
    }

    @Test
    fun `통행료는 운전자 포함 인원으로 나눈다`() {
        commute(november.atDay(2), toll = 1200)

        val receipt = settlementService.issueReceipts(crewId, november).single()

        assertDecimal("600.00", receipt.tollWon)
    }

    @Test
    fun `다시 발행해도 이미 발행된 영수증은 바뀌지 않는다`() {
        commuteDays(november, 2)
        settlementService.issueReceipts(crewId, november)

        val again = settlementService.issueReceipts(crewId, november)

        assertTrue(again.isEmpty())
        assertEquals(1, settlementService.getReceipts(crewId, november).size)
    }

    @Test
    fun `발행 뒤에 늦게 올라온 탑승은 다음 달 영수증에 들어간다`() {
        commute(november.atDay(2))
        settlementService.issueReceipts(crewId, november)
        commute(november.atDay(28)) // 11월 영수증 발행 뒤에 올라온 11월 탑승
        commute(november.plusMonths(1).atDay(1))

        val december = settlementService.issueReceipts(crewId, november.plusMonths(1)).single()

        assertEquals(2, december.items.size)
        assertEquals(
            listOf(LocalDate.of(2026, 11, 28), LocalDate.of(2026, 12, 1)),
            december.items.map { it.serviceDate },
        )
    }

    @Test
    fun `차량 유가가 없으면 그달 공공 유가를 쓴다`() {
        carService.updateCar(jihoon, carId, UpdateCarCommand(FuelType.GASOLINE, null, null))
        commuteDays(november, 1)

        val receipt = settlementService.issueReceipts(crewId, november).single()

        assertEquals(1684, receipt.fuelPriceWon) // V4 초기 데이터의 2026-11 휘발유
    }

    @Test
    fun `차량 유가도 그달 공공 유가도 없으면 FUEL_PRICE_NOT_FOUND`() {
        carService.updateCar(jihoon, carId, UpdateCarCommand(FuelType.GASOLINE, null, null))
        commuteDays(december, 1) // 초기 데이터는 2026-11까지

        val e = assertThrows<BusinessException> { settlementService.issueReceipts(crewId, december) }

        assertEquals(ErrorCode.FUEL_PRICE_NOT_FOUND, e.errorCode)
    }

    @Test
    fun `발행한 영수증을 다시 조회하면 같은 금액이다`() {
        confirmDetour("3.0")
        commuteDays(november, 18)
        val issued = settlementService.issueReceipts(crewId, november).single()

        val found = settlementService.getReceipt(issued.receiptId)

        assertEquals(20_700, found.amountWon)
        assertEquals(18, found.boardingDays)
    }

    @Test
    fun `영수증을 발행하면 내는 사람에게 갈 ReceiptIssuedEvent를 발행한다`() {
        confirmDetour("3.0")
        commuteDays(november, 18)

        val receipt = settlementService.issueReceipts(crewId, november).single()

        val event = events.stream(ReceiptIssuedEvent::class.java).toList().single()
        assertEquals(receipt.receiptId, event.receiptId)
        assertEquals(receipt.payerMemberId, event.payerMemberId)
        assertEquals(20_700, event.amountWon)
        assertEquals(november, event.month)
    }

    @Test
    fun `끝나지 않은 달은 발행할 수 없다`() {
        val e = assertThrows<BusinessException> { settlementService.issueReceipts(crewId, YearMonth.of(2027, 1)) }

        assertEquals(ErrorCode.SETTLEMENT_MONTH_NOT_CLOSED, e.errorCode)
    }

    @Test
    fun `매달 1일 자동 발행은 모든 크루의 지난달 영수증을 발행한다`() {
        commuteDays(december, 2)

        monthlySettlementScheduler.issueLastMonth()

        val receipt = settlementService.getReceipts(crewId, december).single()
        assertEquals(2, receipt.boardingDays)
    }

    @Test
    fun `운전자가 바뀌면 운행에 쓴 차량의 유가로 계산한다`() {
        val suaMemberId = crewService.getCrewMemberEntity(sua).member.id
        memberService.registerBankAccount(suaMemberId, BankAccountCommand("○○은행", "1002-123-456789", "수아"))
        val suaCar = carService.registerCar(suaMemberId, RegisterCarCommand("VK-2", FuelType.EV)).carId
        carService.updateCar(suaMemberId, suaCar, UpdateCarCommand(FuelType.EV, null, 300))

        commute(november.atDay(2))                                                         // 지훈 차(1684원/L), 수아 탑승
        commute(november.atDay(3), driverMemberId = suaMemberId, car = suaCar, rider = driverId) // 수아 차(300원/kWh), 지훈 탑승

        val receipts = settlementService.issueReceipts(crewId, november).associateBy { it.payeeNickname }
        assertEquals(1684, receipts.getValue("지훈").fuelPriceWon)
        assertEquals(300, receipts.getValue("수아").fuelPriceWon)
    }
}
