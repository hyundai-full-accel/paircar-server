package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.car.application.CarService
import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.crew.application.dto.CrewResult
import com.fullaccel.paircarserver.crew.application.event.CrewMemberJoinedEvent
import com.fullaccel.paircarserver.crew.domain.Crew
import com.fullaccel.paircarserver.crew.domain.CrewMember
import com.fullaccel.paircarserver.crew.domain.CrewMemberRepository
import com.fullaccel.paircarserver.crew.domain.CrewRepository
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CrewServiceImpl(
    private val crewRepository: CrewRepository,
    private val crewMemberRepository: CrewMemberRepository,
    private val memberService: MemberService,
    private val carService: CarService,
    private val eventPublisher: ApplicationEventPublisher,
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
        val crewMember = crewMemberRepository.save(CrewMember(crew = crew, member = member))
        val drivers = carService.memberIdsWithCar(
            crewMemberRepository.findAllByCrewIdOrderById(crewId).map { it.member.id }.filter { it != memberId },
        )
        eventPublisher.publishEvent(
            CrewMemberJoinedEvent(
                crewId = crewId,
                crewMemberId = crewMember.id,
                nickname = member.nickname,
                recipientMemberIds = drivers.toList(),
            ),
        )
        return CrewMemberResult.from(crewMember, carService.hasCar(memberId))
    }

    override fun getCrewMembers(crewId: Long): List<CrewMemberResult> {
        getCrewEntity(crewId)
        val members = crewMemberRepository.findAllByCrewIdOrderById(crewId)
        val drivers = carService.memberIdsWithCar(members.map { it.member.id })
        return members.map { CrewMemberResult.from(it, it.member.id in drivers) }
    }

    override fun getCrewsOfMember(memberId: Long): List<CrewResult> =
        crewMemberRepository.findAllByMemberIdOrderById(memberId).map { CrewResult.from(it.crew) }

    override fun getAllCrewIds(): List<Long> = crewRepository.findAllIds()

    override fun validateCrewAccess(crewId: Long, memberId: Long): Long {
        getCrewEntity(crewId)
        return crewMemberRepository.findByCrewIdAndMemberId(crewId, memberId)?.id
            ?: throw BusinessException(ErrorCode.CREW_ACCESS_DENIED)
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
