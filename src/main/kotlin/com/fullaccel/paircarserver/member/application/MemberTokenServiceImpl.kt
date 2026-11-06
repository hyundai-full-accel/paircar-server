package com.fullaccel.paircarserver.member.application

import com.fullaccel.paircarserver.member.domain.MemberToken
import com.fullaccel.paircarserver.member.domain.MemberTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.HexFormat

@Service
@Transactional(readOnly = true)
class MemberTokenServiceImpl(
    private val memberTokenRepository: MemberTokenRepository,
    private val memberService: MemberService,
) : MemberTokenService {

    private val random = SecureRandom()

    @Transactional
    override fun issueToken(memberId: Long): String {
        val member = memberService.getMemberEntity(memberId)
        val rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(TOKEN_BYTES).also(random::nextBytes))
        memberTokenRepository.save(
            MemberToken(
                member = member,
                tokenHash = hash(rawToken),
                expiresAt = Instant.now().plus(TOKEN_LIFETIME),
            ),
        )
        return rawToken
    }

    override fun authenticate(rawToken: String): Long? {
        val token = memberTokenRepository.findByTokenHash(hash(rawToken)) ?: return null
        if (token.revokedAt != null || token.expiresAt.isBefore(Instant.now())) return null
        return token.member.id
    }

    private fun hash(rawToken: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray()))

    companion object {
        private const val TOKEN_BYTES = 32
        private val TOKEN_LIFETIME: Duration = Duration.ofDays(180)
    }
}
