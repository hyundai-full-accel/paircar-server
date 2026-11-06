package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.crew.application.dto.InviteJoinResult
import com.fullaccel.paircarserver.crew.application.dto.InviteResult
import com.fullaccel.paircarserver.crew.domain.CrewInvite
import com.fullaccel.paircarserver.crew.domain.CrewInviteRepository
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import com.fullaccel.paircarserver.member.application.MemberTokenService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Service
@Transactional(readOnly = true)
class CrewInviteServiceImpl(
    private val crewInviteRepository: CrewInviteRepository,
    private val crewService: CrewService,
    private val memberService: MemberService,
    private val memberTokenService: MemberTokenService,
) : CrewInviteService {

    private val random = SecureRandom()

    @Transactional
    override fun createInvite(crewId: Long, requesterMemberId: Long): InviteResult {
        crewService.validateCrewAccess(crewId, requesterMemberId)
        val invite = CrewInvite(
            crew = crewService.getCrewEntity(crewId),
            code = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(CODE_BYTES).also(random::nextBytes)),
            expiresAt = Instant.now().plus(INVITE_LIFETIME),
        )
        return InviteResult.from(crewInviteRepository.save(invite))
    }

    override fun getInvite(code: String): InviteResult = InviteResult.from(getValidInvite(code))

    @Transactional
    override fun joinAsNewMember(code: String, nickname: String): InviteJoinResult {
        val invite = getValidInvite(code)
        val member = memberService.createMember(nickname)
        val crewMember = crewService.addMember(invite.crew.id, member.memberId)
        return InviteJoinResult(
            memberId = member.memberId,
            crewMemberId = crewMember.crewMemberId,
            crewId = invite.crew.id,
            token = memberTokenService.issueToken(member.memberId),
        )
    }

    @Transactional
    override fun joinAsMember(code: String, memberId: Long): CrewMemberResult {
        val invite = getValidInvite(code)
        return crewService.addMember(invite.crew.id, memberId)
    }

    private fun getValidInvite(code: String): CrewInvite {
        val invite = crewInviteRepository.findByCode(code) ?: throw BusinessException(ErrorCode.INVITE_NOT_FOUND)
        if (invite.expiresAt.isBefore(Instant.now())) throw BusinessException(ErrorCode.INVITE_EXPIRED)
        return invite
    }

    companion object {
        private const val CODE_BYTES = 9 // Base64 URL 12자
        private val INVITE_LIFETIME: Duration = Duration.ofDays(7)
    }
}
