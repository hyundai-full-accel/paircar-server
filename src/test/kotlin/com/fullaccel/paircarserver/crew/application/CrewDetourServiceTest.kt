package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.crew.application.dto.RegisterDetourCommand
import com.fullaccel.paircarserver.crew.application.dto.RegisterPlaceCommand
import com.fullaccel.paircarserver.crew.application.event.DetourConfirmationRequestedEvent
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@IntegrationTest
@RecordApplicationEvents
class CrewDetourServiceTest(
    @Autowired private val crewDetourService: CrewDetourService,
    @Autowired private val crewPlaceService: CrewPlaceService,
    @Autowired private val crewService: CrewService,
    @Autowired private val memberService: MemberService,
) {

    @Autowired
    private lateinit var events: ApplicationEvents

    private var crewId = 0L
    private var driverId = 0L
    private var passengerId = 0L
    private var placeId = 0L

    @BeforeEach
    fun setUp() {
        val jihoon = memberService.createMember("지훈")
        val sua = memberService.createMember("수아")
        crewId = crewService.createCrew(jihoon.memberId, "판교 출근 크루").crewId
        driverId = crewService.getCrewMemberEntity(crewId, jihoon.memberId).id
        passengerId = crewService.addMember(crewId, sua.memberId).crewMemberId
        placeId = crewPlaceService.registerPlace(
            crewId,
            RegisterPlaceCommand("○○아파트 정문", BigDecimal("37.401000"), BigDecimal("127.108000")),
        ).placeId
    }

    private fun command(km: String = "3.0") = RegisterDetourCommand(passengerId, driverId, placeId, BigDecimal(km))

    @Test
    fun `우회 거리를 등록하면 확인 전 상태다`() {
        val detour = crewDetourService.registerDetour(crewId, command())

        assertEquals(0, BigDecimal("3.0").compareTo(detour.detourKm))
        assertFalse(detour.confirmed)
    }

    @Test
    fun `운전자와 동승자가 둘 다 확인해야 confirmed`() {
        val detour = crewDetourService.registerDetour(crewId, command())

        assertFalse(crewDetourService.confirmDetour(detour.detourId, driverId).confirmed)
        assertTrue(crewDetourService.confirmDetour(detour.detourId, passengerId).confirmed)
    }

    @Test
    fun `값이 바뀌면 확인이 초기화된다`() {
        val detour = crewDetourService.registerDetour(crewId, command("3.0"))
        crewDetourService.confirmDetour(detour.detourId, driverId)
        crewDetourService.confirmDetour(detour.detourId, passengerId)

        val changed = crewDetourService.registerDetour(crewId, command("3.4"))

        assertEquals(detour.detourId, changed.detourId)
        assertNull(changed.driverConfirmedAt)
        assertFalse(changed.confirmed)
    }

    @Test
    fun `같은 값으로 다시 등록하면 확인이 유지된다`() {
        val detour = crewDetourService.registerDetour(crewId, command("3.0"))
        crewDetourService.confirmDetour(detour.detourId, driverId)

        val same = crewDetourService.registerDetour(crewId, command("3.00"))

        assertEquals(detour.detourId, same.detourId)
        assertTrue(same.driverConfirmedAt != null)
    }

    @Test
    fun `당사자가 아닌 크루 멤버가 확인하면 DETOUR_CONFIRMER_NOT_PARTY`() {
        val other = memberService.createMember("민준")
        val otherId = crewService.addMember(crewId, other.memberId).crewMemberId
        val detour = crewDetourService.registerDetour(crewId, command())

        val e = assertThrows<BusinessException> { crewDetourService.confirmDetour(detour.detourId, otherId) }

        assertEquals(ErrorCode.DETOUR_CONFIRMER_NOT_PARTY, e.errorCode)
    }

    @Test
    fun `동승자와 운전자가 같으면 DETOUR_SAME_PASSENGER_AND_DRIVER`() {
        val e = assertThrows<BusinessException> {
            crewDetourService.registerDetour(crewId, RegisterDetourCommand(driverId, driverId, placeId, BigDecimal("3.0")))
        }

        assertEquals(ErrorCode.DETOUR_SAME_PASSENGER_AND_DRIVER, e.errorCode)
    }

    @Test
    fun `다른 크루의 멤버면 CREW_MISMATCH`() {
        val outsider = memberService.createMember("외부")
        val otherCrew = crewService.createCrew(outsider.memberId, "다른 크루")
        val outsiderId = crewService.getCrewMemberEntity(otherCrew.crewId, outsider.memberId).id

        val e = assertThrows<BusinessException> {
            crewDetourService.registerDetour(crewId, RegisterDetourCommand(outsiderId, driverId, placeId, BigDecimal("3.0")))
        }

        assertEquals(ErrorCode.CREW_MISMATCH, e.errorCode)
    }

    @Test
    fun `동승자, 운전자, 장소로 우회 거리를 찾는다`() {
        crewDetourService.registerDetour(crewId, command())

        assertEquals(1, crewDetourService.getDetours(crewId).size)
        assertTrue(crewDetourService.findDetour(passengerId, driverId, placeId) != null)
        assertNull(crewDetourService.findDetour(driverId, passengerId, placeId))
    }

    @Test
    fun `새로 등록하거나 값이 바뀔 때만 확인 요청 이벤트를 발행한다`() {
        crewDetourService.registerDetour(crewId, command("3.0"))
        crewDetourService.registerDetour(crewId, command("3.00")) // 같은 값
        crewDetourService.registerDetour(crewId, command("3.4"))

        val published = events.stream(DetourConfirmationRequestedEvent::class.java).toList()
        assertEquals(listOf("3.0", "3.4"), published.map { it.detourKm.toPlainString() })
        assertEquals("○○아파트 정문", published.first().placeName)
    }
}
