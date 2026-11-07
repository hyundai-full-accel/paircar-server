package com.fullaccel.paircarserver.crew.application.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fullaccel.paircarserver.crew.domain.CrewInvite
import com.fullaccel.paircarserver.crew.domain.SettlementMode
import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

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

// 초대 링크를 연 사람(아직 토큰 없음)에게 보여 줄 크루 소개 (동승자 웹 참여 1단계)
// 크루원 이름은 운전자만 보여 주고, 나머지는 인원 수만 줌
// 장소는 참여하면서 평소 탈 곳, 내릴 곳을 고를 수 있게 함께 줌
data class InvitePreviewResult(
    val code: String,
    val crewId: Long,
    val crewName: String,
    val expiresAt: Instant,
    val settlementMode: SettlementMode,
    val includeDriver: Boolean,
    @field:Schema(description = "평소 출발 시각 (KST, HH:mm)", type = "string", example = "07:10")
    @field:JsonFormat(pattern = "HH:mm")
    val departureTime: LocalTime?,
    @field:Schema(description = "평소 도착 시각 (KST, HH:mm)", type = "string", example = "08:32")
    @field:JsonFormat(pattern = "HH:mm")
    val arrivalTime: LocalTime?,
    val departureDays: List<DayOfWeek>,
    val originPlace: CrewPlaceResult?,
    val destinationPlace: CrewPlaceResult?,
    @field:Schema(description = "지금 크루 인원 (운전자 포함)", example = "3")
    val memberCount: Int,
    @field:Schema(description = "차를 등록한 크루원 이름 (운전자)", example = "[\"민혁\"]")
    val driverNicknames: List<String>,
    @field:Schema(description = "크루 장소 (평소 탈 곳, 내릴 곳 고르기)")
    val places: List<CrewPlaceResult>,
) {
    companion object {
        fun of(invite: CrewInvite, crew: CrewResult, members: List<CrewMemberResult>, places: List<CrewPlaceResult>) = InvitePreviewResult(
            code = invite.code,
            crewId = crew.crewId,
            crewName = crew.name,
            expiresAt = invite.expiresAt,
            settlementMode = crew.settlementMode,
            includeDriver = crew.includeDriver,
            departureTime = crew.departureTime,
            arrivalTime = crew.arrivalTime,
            departureDays = crew.departureDays,
            originPlace = crew.originPlace,
            destinationPlace = crew.destinationPlace,
            memberCount = members.size,
            driverNicknames = members.filter { it.driver }.map { it.nickname },
            places = places,
        )
    }
}
