package com.fullaccel.paircarserver.crew.application.dto

import com.fullaccel.paircarserver.crew.domain.CrewInvite
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class InviteResult(
    @field:Schema(description = "초대 코드 (대문자, 숫자 8자). 직접 입력할 때는 대소문자와 하이픈을 무시함", example = "K7M2QX9P")
    val code: String,
    @field:Schema(description = "초대 링크. 공유하거나 QR로 그릴 때 그대로 쓰세요", example = "https://paircar.app/join/K7M2QX9P")
    val inviteUrl: String,
    val crewId: Long,
    val crewName: String,
    val expiresAt: Instant,
) {
    companion object {
        fun of(invite: CrewInvite, inviteUrl: String) = InviteResult(
            code = invite.code,
            inviteUrl = inviteUrl,
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
