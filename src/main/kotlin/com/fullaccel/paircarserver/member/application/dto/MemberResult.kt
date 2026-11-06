package com.fullaccel.paircarserver.member.application.dto

import com.fullaccel.paircarserver.member.domain.Member
import java.time.Instant

data class MemberResult(
    val memberId: Long,
    val nickname: String,
    val createdAt: Instant,
) {
    companion object {
        fun from(member: Member) = MemberResult(
            memberId = member.id,
            nickname = member.nickname,
            createdAt = member.createdAt,
        )
    }
}
