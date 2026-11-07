package com.fullaccel.paircarserver.crew.application.dto

import com.fullaccel.paircarserver.crew.domain.CrewMember
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class CrewMemberResult(
    val crewMemberId: Long,
    val crewId: Long,
    val memberId: Long,
    val nickname: String,
    val joinedAt: Instant,
    @field:Schema(description = "동승자로 탈 때 평소 탈 곳")
    val boardPlace: CrewPlaceResult?,
    @field:Schema(description = "동승자로 탈 때 평소 내릴 곳")
    val alightPlace: CrewPlaceResult?,
) {
    companion object {
        fun from(crewMember: CrewMember) = CrewMemberResult(
            crewMemberId = crewMember.id,
            crewId = crewMember.crew.id,
            memberId = crewMember.member.id,
            nickname = crewMember.member.nickname,
            joinedAt = crewMember.joinedAt,
            boardPlace = crewMember.boardPlace?.let(CrewPlaceResult::from),
            alightPlace = crewMember.alightPlace?.let(CrewPlaceResult::from),
        )
    }
}
