package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.crew.application.dto.RegisterPlaceCommand
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import kotlin.test.assertEquals

@IntegrationTest
class CrewPlaceServiceTest(
    @Autowired private val crewPlaceService: CrewPlaceService,
    @Autowired private val crewService: CrewService,
    @Autowired private val memberService: MemberService,
) {

    private fun newCrew(name: String = "판교 출근 크루"): Long {
        val member = memberService.createMember("지훈")
        return crewService.createCrew(member.memberId, name).crewId
    }

    private fun place(name: String) = RegisterPlaceCommand(name, BigDecimal("37.401000"), BigDecimal("127.108000"))

    @Test
    fun `장소를 등록하고 등록 순서대로 조회한다`() {
        val crewId = newCrew()

        crewPlaceService.registerPlace(crewId, place("○○아파트 정문"))
        crewPlaceService.registerPlace(crewId, place("판교 본관"))

        assertEquals(listOf("○○아파트 정문", "판교 본관"), crewPlaceService.getPlaces(crewId).map { it.name })
    }

    @Test
    fun `같은 크루에 같은 이름을 등록하면 CREW_PLACE_NAME_DUPLICATED`() {
        val crewId = newCrew()
        crewPlaceService.registerPlace(crewId, place("판교 본관"))

        val e = assertThrows<BusinessException> { crewPlaceService.registerPlace(crewId, place("판교 본관")) }

        assertEquals(ErrorCode.CREW_PLACE_NAME_DUPLICATED, e.errorCode)
    }

    @Test
    fun `다른 크루면 같은 이름을 등록할 수 있다`() {
        val crewA = newCrew("A")
        val crewB = newCrew("B")

        crewPlaceService.registerPlace(crewA, place("판교 본관"))
        crewPlaceService.registerPlace(crewB, place("판교 본관"))

        assertEquals(1, crewPlaceService.getPlaces(crewB).size)
    }

    @Test
    fun `다른 크루의 장소를 크루 기준으로 찾으면 CREW_MISMATCH`() {
        val crewA = newCrew("A")
        val crewB = newCrew("B")
        val placeOfA = crewPlaceService.registerPlace(crewA, place("판교 본관"))

        val e = assertThrows<BusinessException> { crewPlaceService.getPlaceEntity(crewB, placeOfA.placeId) }

        assertEquals(ErrorCode.CREW_MISMATCH, e.errorCode)
    }
}
