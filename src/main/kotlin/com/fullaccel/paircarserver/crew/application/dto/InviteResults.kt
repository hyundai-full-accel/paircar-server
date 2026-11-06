package com.fullaccel.paircarserver.crew.application.dto

import com.fullaccel.paircarserver.crew.domain.CrewInvite
import java.time.Instant

data class InviteResult(
    val code: String,
    val crewId: Long,
    val crewName: String,
    val expiresAt: Instant,
) {
    companion object {
        fun from(invite: CrewInvite) = InviteResult(
            code = invite.code,
            crewId = invite.crew.id,
            crewName = invite.crew.name,
            expiresAt = invite.expiresAt,
        )
    }
}

// 초대 링크로 새 멤버가 참여한 결과. token은 이때 한 번만 받을 수 있음
data class InviteJoinResult(
    val memberId: Long,
    val crewMemberId: Long,
    val crewId: Long,
    val token: String,
)
