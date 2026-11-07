package com.fullaccel.paircarserver.crew.application.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fullaccel.paircarserver.crew.domain.CrewMember
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalTime

data class CrewMemberResult(
    val crewMemberId: Long,
    val crewId: Long,
    val memberId: Long,
    val nickname: String,
    val joinedAt: Instant,
    @field:Schema(description = "차를 등록한 멤버 (운전할 수 있음). 화면의 운전자/동승자 표시. 실제 운전자는 운행마다 정해짐")
    val driver: Boolean,
    @field:Schema(description = "동승자로 탈 때 평소 탈 곳")
    val boardPlace: CrewPlaceResult?,
    @field:Schema(description = "동승자로 탈 때 평소 내릴 곳")
    val alightPlace: CrewPlaceResult?,
    @field:Schema(description = "평소 픽업 시각 (KST, HH:mm)", type = "string", example = "07:32")
    @field:JsonFormat(pattern = "HH:mm")
    val pickupTime: LocalTime?,
    @field:Schema(description = "정산 기준에 동의한 시각")
    val settlementAgreedAt: Instant?,
    @field:Schema(description = "지금 정산 방식에 동의했는지. 동의 뒤 정산 방식이 바뀌면 false")
    val settlementAgreed: Boolean,
) {
    companion object {
        fun from(crewMember: CrewMember, driver: Boolean) = CrewMemberResult(
            crewMemberId = crewMember.id,
            crewId = crewMember.crew.id,
            memberId = crewMember.member.id,
            nickname = crewMember.member.nickname,
            joinedAt = crewMember.joinedAt,
            driver = driver,
            boardPlace = crewMember.boardPlace?.let(CrewPlaceResult::from),
            alightPlace = crewMember.alightPlace?.let(CrewPlaceResult::from),
            pickupTime = crewMember.pickupTime,
            settlementAgreedAt = crewMember.settlementAgreedAt,
            settlementAgreed = crewMember.crew.isSettlementAgreed(crewMember.settlementAgreedAt),
        )
    }
}
