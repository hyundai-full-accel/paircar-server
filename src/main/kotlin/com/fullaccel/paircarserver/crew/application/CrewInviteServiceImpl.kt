package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.crew.application.dto.InviteJoinResult
import com.fullaccel.paircarserver.crew.application.dto.InvitePreviewResult
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

@Service
@Transactional(readOnly = true)
class CrewInviteServiceImpl(
    private val crewInviteRepository: CrewInviteRepository,
    private val crewService: CrewService,
    private val crewPlaceService: CrewPlaceService,
    private val memberService: MemberService,
    private val memberTokenService: MemberTokenService,
    private val inviteProperties: InviteProperties,
) : CrewInviteService {

    private val random = SecureRandom()

    @Transactional
    override fun createInvite(crewId: Long, requesterMemberId: Long): InviteResult {
        crewService.validateCrewAccess(crewId, requesterMemberId)
        val invite = CrewInvite(
            crew = crewService.getCrewEntity(crewId),
            code = generateUniqueCode(),
            expiresAt = Instant.now().plus(INVITE_LIFETIME),
        )
        return toResult(crewInviteRepository.save(invite))
    }

    override fun getInvite(code: String): InvitePreviewResult {
        val invite = getValidInvite(code)
        val crewId = invite.crew.id
        return InvitePreviewResult.of(
            invite = invite,
            crew = crewService.getCrew(crewId),
            members = crewService.getCrewMembers(crewId),
            places = crewPlaceService.getPlaces(crewId),
        )
    }

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

    // 사람이 입력한 코드도 받도록 대소문자, 하이픈, 공백을 무시함 (k7m2-qx9p → K7M2QX9P)
    private fun getValidInvite(code: String): CrewInvite {
        val normalized = code.uppercase().filter { it != '-' && !it.isWhitespace() }
        val invite = crewInviteRepository.findByCode(normalized) ?: throw BusinessException(ErrorCode.INVITE_NOT_FOUND)
        if (invite.expiresAt.isBefore(Instant.now())) throw BusinessException(ErrorCode.INVITE_EXPIRED)
        return invite
    }

    private fun generateUniqueCode(): String {
        repeat(MAX_CODE_ATTEMPTS) {
            val code = (1..CODE_LENGTH).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")
            if (!crewInviteRepository.existsByCode(code)) return code
        }
        throw IllegalStateException("초대 코드를 만들지 못했습니다.")
    }

    private fun toResult(invite: CrewInvite) = InviteResult.of(invite, inviteProperties.inviteUrl(invite.code))

    companion object {
        // 헷갈리는 0, 1, I, L, O를 뺀 31자. 8자면 약 8,500억 가지
        private const val CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
        private const val CODE_LENGTH = 8
        private const val MAX_CODE_ATTEMPTS = 5
        private val INVITE_LIFETIME: Duration = Duration.ofDays(7)
    }
}
