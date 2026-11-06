package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.crew.application.dto.CrewResult
import com.fullaccel.paircarserver.crew.domain.Crew
import com.fullaccel.paircarserver.crew.domain.CrewMember
import com.fullaccel.paircarserver.crew.domain.CrewMemberRepository
import com.fullaccel.paircarserver.crew.domain.CrewRepository
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CrewServiceImpl(
    private val crewRepository: CrewRepository,
    private val crewMemberRepository: CrewMemberRepository,
    private val memberService: MemberService,
) : CrewService {

    @Transactional
    override fun createCrew(creatorMemberId: Long, name: String): CrewResult {
        val creator = memberService.getMemberEntity(creatorMemberId)
        val crew = crewRepository.save(Crew(name = name))
        crewMemberRepository.save(CrewMember(crew = crew, member = creator))
        return CrewResult.from(crew)
    }

    override fun getCrew(crewId: Long): CrewResult = CrewResult.from(getCrewEntity(crewId))

    @Transactional
    override fun addMember(crewId: Long, memberId: Long): CrewMemberResult {
        val crew = getCrewEntity(crewId)
        val member = memberService.getMemberEntity(memberId)
        if (crewMemberRepository.existsByCrewIdAndMemberId(crewId, memberId)) {
            throw BusinessException(ErrorCode.CREW_MEMBER_ALREADY_EXISTS)
        }
        return CrewMemberResult.from(crewMemberRepository.save(CrewMember(crew = crew, member = member)))
    }

    override fun getCrewMembers(crewId: Long): List<CrewMemberResult> {
        getCrewEntity(crewId)
        return crewMemberRepository.findAllByCrewIdOrderById(crewId).map(CrewMemberResult::from)
    }

    @Transactional
    override fun changeManualFuelPrice(crewId: Long, priceWon: Int?): CrewResult {
        val crew = getCrewEntity(crewId)
        crew.changeManualFuelPrice(priceWon)
        return CrewResult.from(crew)
    }

    override fun getCrewEntity(crewId: Long): Crew =
        crewRepository.findByIdOrNull(crewId)
            ?: throw BusinessException(ErrorCode.CREW_NOT_FOUND)

    override fun getCrewMemberEntity(crewMemberId: Long): CrewMember =
        crewMemberRepository.findByIdOrNull(crewMemberId)
            ?: throw BusinessException(ErrorCode.CREW_MEMBER_NOT_FOUND)

    override fun getCrewMemberEntity(crewId: Long, memberId: Long): CrewMember =
        crewMemberRepository.findByCrewIdAndMemberId(crewId, memberId)
            ?: throw BusinessException(ErrorCode.CREW_MEMBER_NOT_FOUND)
}
