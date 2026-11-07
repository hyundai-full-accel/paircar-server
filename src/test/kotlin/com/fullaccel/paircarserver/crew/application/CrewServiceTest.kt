package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

@IntegrationTest
class CrewServiceTest(
    @Autowired private val crewService: CrewService,
    @Autowired private val memberService: MemberService,
) {

    @Test
    fun `크루를 만들면 만든 사람이 자동으로 참여한다`() {
        val jihoon = memberService.createMember("지훈")

        val crew = crewService.createCrew(jihoon.memberId, "판교 출근 크루")

        assertEquals(listOf("지훈"), crewService.getCrewMembers(crew.crewId).map { it.nickname })
    }

    @Test
    fun `멤버를 추가하면 참여 순서대로 조회된다`() {
        val jihoon = memberService.createMember("지훈")
        val sua = memberService.createMember("수아")
        val crew = crewService.createCrew(jihoon.memberId, "판교 출근 크루")

        crewService.addMember(crew.crewId, sua.memberId)

        assertEquals(listOf("지훈", "수아"), crewService.getCrewMembers(crew.crewId).map { it.nickname })
    }

    @Test
    fun `이미 참여한 멤버를 추가하면 CREW_MEMBER_ALREADY_EXISTS`() {
        val jihoon = memberService.createMember("지훈")
        val crew = crewService.createCrew(jihoon.memberId, "판교 출근 크루")

        val e = assertThrows<BusinessException> { crewService.addMember(crew.crewId, jihoon.memberId) }

        assertEquals(ErrorCode.CREW_MEMBER_ALREADY_EXISTS, e.errorCode)
    }

    @Test
    fun `크루에 없는 멤버를 찾으면 CREW_MEMBER_NOT_FOUND`() {
        val jihoon = memberService.createMember("지훈")
        val sua = memberService.createMember("수아")
        val crew = crewService.createCrew(jihoon.memberId, "판교 출근 크루")

        val e = assertThrows<BusinessException> { crewService.getCrewMemberEntity(crew.crewId, sua.memberId) }

        assertEquals(ErrorCode.CREW_MEMBER_NOT_FOUND, e.errorCode)
    }

    @Test
    fun `없는 크루를 조회하면 CREW_NOT_FOUND`() {
        val e = assertThrows<BusinessException> { crewService.getCrew(999_999L) }

        assertEquals(ErrorCode.CREW_NOT_FOUND, e.errorCode)
    }
}
