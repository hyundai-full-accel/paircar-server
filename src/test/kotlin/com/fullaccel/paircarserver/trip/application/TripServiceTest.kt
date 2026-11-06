package com.fullaccel.paircarserver.trip.application

import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.car.application.CarService
import com.fullaccel.paircarserver.car.application.dto.RegisterCarCommand
import com.fullaccel.paircarserver.car.domain.FuelType
import com.fullaccel.paircarserver.crew.application.CrewPlaceService
import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.crew.application.dto.RegisterPlaceCommand
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import com.fullaccel.paircarserver.member.application.dto.BankAccountCommand
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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@IntegrationTest
class TripServiceTest(
    @Autowired private val tripService: TripService,
    @Autowired private val memberService: MemberService,
    @Autowired private val carService: CarService,
    @Autowired private val crewService: CrewService,
    @Autowired private val crewPlaceService: CrewPlaceService,
) {

    private var jihoon = 0L // 운전자 멤버 ID
    private var crewId = 0L
    private var carId = 0L
    private var sua = 0L    // 동승자 크루 멤버 ID
    private var minjun = 0L // 동승자 크루 멤버 ID
    private var home = 0L
    private var gate = 0L
    private var backGate = 0L
    private var office = 0L

    @BeforeEach
    fun setUp() {
        jihoon = memberService.createMember("지훈").memberId
        memberService.registerBankAccount(jihoon, BankAccountCommand("○○은행", "3333-01-1234567", "지훈"))
        carId = carService.registerCar(jihoon, RegisterCarCommand("VK-1", FuelType.GASOLINE)).carId
        crewId = crewService.createCrew(jihoon, "판교 출근 크루").crewId
        sua = crewService.addMember(crewId, memberService.createMember("수아").memberId).crewMemberId
        minjun = crewService.addMember(crewId, memberService.createMember("민준").memberId).crewMemberId
        home = place("지훈 집")
        gate = place("○○아파트 정문")
        backGate = place("○○아파트 후문")
        office = place("판교 본관")
    }

    private fun place(name: String) =
        crewPlaceService.registerPlace(crewId, RegisterPlaceCommand(name, BigDecimal("37.4"), BigDecimal("127.1"))).placeId

    private fun km(value: String) = BigDecimal(value)

    private fun start(vararg riders: RiderCommand, startedAt: Instant = Instant.parse("2026-11-06T23:30:00Z")) =
        tripService.startTrip(
            jihoon,
            StartTripCommand(crewId, carId, home, office, startedAt, km("10000.0"), riders.toList()),
        )

    private fun event(odometer: String, at: String = "2026-11-07T00:00:00Z") =
        BoardingEventCommand(Instant.parse(at), km(odometer))

    private fun segment(seq: Int, from: String, to: String, vararg riders: Long) =
        SegmentCommand(seq, km(from), km(to), km("13.20"), FuelEconomySource.MEASURED, riders.toList())

    @Test
    fun `운행을 시작하면 탑승 행과 경유지가 생기고 운행일은 KST 기준이다`() {
        val trip = start(RiderCommand(sua, gate, office))

        assertEquals(listOf(gate), trip.waypoints.map { it.placeId })
        assertEquals(listOf(sua), trip.boardings.map { it.crewMemberId })
        assertNull(trip.boardings.single().boardedAt)
        assertEquals(LocalDate.of(2026, 11, 7), trip.serviceDate) // UTC 11-06 23:30 = KST 11-07 08:30
    }

    @Test
    fun `운전자 계좌가 없으면 BANK_ACCOUNT_REQUIRED`() {
        val noAccount = memberService.createMember("계좌없음").memberId
        carService.registerCar(noAccount, RegisterCarCommand("VK-1", FuelType.GASOLINE))
        crewService.addMember(crewId, noAccount)

        val e = assertThrows<BusinessException> {
            tripService.startTrip(
                noAccount,
                StartTripCommand(crewId, carId, home, office, Instant.now(), km("10000.0"), emptyList()),
            )
        }

        assertEquals(ErrorCode.BANK_ACCOUNT_REQUIRED, e.errorCode)
    }

    @Test
    fun `등록하지 않은 차량이면 CAR_NOT_REGISTERED_TO_MEMBER`() {
        val otherCar = carService.registerCar(memberService.createMember("남").memberId, RegisterCarCommand("VK-2", FuelType.EV))

        val e = assertThrows<BusinessException> {
            tripService.startTrip(
                jihoon,
                StartTripCommand(crewId, otherCar.carId, home, office, Instant.now(), km("10000.0"), emptyList()),
            )
        }

        assertEquals(ErrorCode.CAR_NOT_REGISTERED_TO_MEMBER, e.errorCode)
    }

    @Test
    fun `운전자를 동승자로 넣으면 BOARDING_DRIVER_NOT_ALLOWED`() {
        val driverCrewMemberId = crewService.getCrewMemberEntity(crewId, jihoon).id

        val e = assertThrows<BusinessException> { start(RiderCommand(driverCrewMemberId, gate, office)) }

        assertEquals(ErrorCode.BOARDING_DRIVER_NOT_ALLOWED, e.errorCode)
    }

    @Test
    fun `경유지가 3곳이면 TOO_MANY_WAYPOINTS`() {
        val third = place("역 앞")

        val e = assertThrows<BusinessException> {
            start(RiderCommand(sua, gate, office), RiderCommand(minjun, backGate, third))
        }

        assertEquals(ErrorCode.TOO_MANY_WAYPOINTS, e.errorCode)
    }

    @Test
    fun `진행 중인 운행이 있으면 TRIP_ALREADY_IN_PROGRESS`() {
        start(RiderCommand(sua, gate, office))

        val e = assertThrows<BusinessException> { start(RiderCommand(sua, gate, office)) }

        assertEquals(ErrorCode.TRIP_ALREADY_IN_PROGRESS, e.errorCode)
    }

    @Test
    fun `같은 탑승 이벤트가 다시 와도 처음 값을 유지한다`() {
        val trip = start(RiderCommand(sua, gate, office))

        tripService.recordBoarding(jihoon, trip.tripId, sua, event("10003.0"))
        val again = tripService.recordBoarding(jihoon, trip.tripId, sua, event("10009.0"))

        assertEquals(0, km("10003.0").compareTo(again.boardings.single().boardOdometerKm))
    }

    @Test
    fun `탑승 전에 하차하면 BOARDING_NOT_BOARDED`() {
        val trip = start(RiderCommand(sua, gate, office))

        val e = assertThrows<BusinessException> { tripService.recordAlighting(jihoon, trip.tripId, sua, event("10015.0")) }

        assertEquals(ErrorCode.BOARDING_NOT_BOARDED, e.errorCode)
    }

    @Test
    fun `탑승 주행거리가 출발보다 작으면 INVALID_ODOMETER`() {
        val trip = start(RiderCommand(sua, gate, office))

        val e = assertThrows<BusinessException> { tripService.recordBoarding(jihoon, trip.tripId, sua, event("9999.0")) }

        assertEquals(ErrorCode.INVALID_ODOMETER, e.errorCode)
    }

    @Test
    fun `탑승 전이면 불참 처리로 탑승 행이 사라지고 탑승 후면 BOARDING_ALREADY_BOARDED`() {
        val trip = start(RiderCommand(sua, gate, office), RiderCommand(minjun, gate, office))

        val afterCancel = tripService.cancelBoarding(jihoon, trip.tripId, minjun)
        assertEquals(listOf(sua), afterCancel.boardings.map { it.crewMemberId })

        tripService.recordBoarding(jihoon, trip.tripId, sua, event("10003.0"))
        val e = assertThrows<BusinessException> { tripService.cancelBoarding(jihoon, trip.tripId, sua) }
        assertEquals(ErrorCode.BOARDING_ALREADY_BOARDED, e.errorCode)
    }

    @Test
    fun `탈 지점을 바꾸면 경유지가 바뀐다`() {
        val trip = start(RiderCommand(sua, gate, office))

        val changed = tripService.changeBoardPlace(jihoon, trip.tripId, sua, backGate)

        assertEquals(listOf(backGate), changed.waypoints.map { it.placeId })
    }

    @Test
    fun `운전자가 아닌 멤버가 요청하면 TRIP_ACCESS_DENIED`() {
        val trip = start(RiderCommand(sua, gate, office))
        val outsider = memberService.createMember("남").memberId

        val e = assertThrows<BusinessException> { tripService.recordBoarding(outsider, trip.tripId, sua, event("10003.0")) }

        assertEquals(ErrorCode.TRIP_ACCESS_DENIED, e.errorCode)
    }

    @Test
    fun `구간을 올리면 구간별 동승자가 기록되고 다시 올리면 건너뛴다`() {
        val trip = start(RiderCommand(sua, gate, office))
        tripService.recordBoarding(jihoon, trip.tripId, sua, event("10003.0"))

        tripService.uploadSegments(jihoon, trip.tripId, listOf(segment(1, "10000.0", "10003.0"), segment(2, "10003.0", "10015.0", sua)))
        val again = tripService.uploadSegments(jihoon, trip.tripId, listOf(segment(2, "10003.0", "10015.0", sua)))

        assertEquals(listOf(1, 2), again.segments.map { it.seq })
        assertEquals(listOf(emptyList(), listOf(sua)), again.segments.map { it.riderCrewMemberIds })
    }

    @Test
    fun `구간이 끊기면 INVALID_SEGMENT`() {
        val trip = start(RiderCommand(sua, gate, office))
        tripService.recordBoarding(jihoon, trip.tripId, sua, event("10003.0"))

        val e = assertThrows<BusinessException> {
            tripService.uploadSegments(jihoon, trip.tripId, listOf(segment(1, "10000.0", "10003.0"), segment(2, "10004.0", "10015.0", sua)))
        }

        assertEquals(ErrorCode.INVALID_SEGMENT, e.errorCode)
    }

    @Test
    fun `탑승하지 않은 동승자를 구간에 넣으면 INVALID_SEGMENT`() {
        val trip = start(RiderCommand(sua, gate, office))

        val e = assertThrows<BusinessException> {
            tripService.uploadSegments(jihoon, trip.tripId, listOf(segment(1, "10000.0", "10015.0", sua)))
        }

        assertEquals(ErrorCode.INVALID_SEGMENT, e.errorCode)
    }

    @Test
    fun `운행을 끝내면 하차 기록이 없는 동승자를 도착 값으로 하차 처리하고 다시 끝내도 그대로다`() {
        val trip = start(RiderCommand(sua, gate, office), RiderCommand(minjun, gate, office))
        tripService.recordBoarding(jihoon, trip.tripId, sua, event("10003.0"))

        val ended = tripService.endTrip(jihoon, trip.tripId, EndTripCommand(Instant.parse("2026-11-07T00:20:00Z"), km("10015.0"), 1200))
        val again = tripService.endTrip(jihoon, trip.tripId, EndTripCommand(Instant.parse("2026-11-07T01:00:00Z"), km("10099.0")))

        val suaBoarding = ended.boardings.first { it.crewMemberId == sua }
        val minjunBoarding = ended.boardings.first { it.crewMemberId == minjun }
        assertEquals(0, km("10015.0").compareTo(suaBoarding.alightOdometerKm))
        assertNull(minjunBoarding.boardedAt) // 안 탄 동승자는 그대로 미탑승
        assertEquals(1200, again.tollFareWon)
        assertEquals(0, km("10015.0").compareTo(again.endOdometerKm))
    }
}
