package com.fullaccel.paircarserver.crew.application.dto

import com.fullaccel.paircarserver.crew.domain.CrewMember
import java.time.Instant

data class CrewMemberResult(
    val crewMemberId: Long,
    val crewId: Long,
    val memberId: Long,
    val nickname: String,
    val joinedAt: Instant,
) {
    companion object {
        fun from(crewMember: CrewMember) = CrewMemberResult(
            crewMemberId = crewMember.id,
            crewId = crewMember.crew.id,
            memberId = crewMember.member.id,
            nickname = crewMember.member.nickname,
            joinedAt = crewMember.joinedAt,
        )
    }
}
