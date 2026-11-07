package com.fullaccel.paircarserver.crew.api

import com.fullaccel.paircarserver.crew.api.dto.JoinAsNewMemberRequest
import com.fullaccel.paircarserver.crew.application.CrewInviteService
import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.crew.application.dto.InviteJoinResult
import com.fullaccel.paircarserver.crew.application.dto.InvitePreviewResult
import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "초대", description = "초대 링크로 크루 참여 (동승자 웹)")
@RestController
@RequestMapping("/api/invites/{code}")
class InviteController(
    private val crewInviteService: CrewInviteService,
) {

    @Operation(
        summary = "초대 조회",
        description = "참여 화면에 보여 줄 크루 소개입니다 (이름, 일정, 출발지·목적지, 인원, 운전자 이름, 정산 방식, 크루 장소). 인증이 필요 없습니다.",
    )
    @SecurityRequirements
    @GetMapping
    fun getInvite(@PathVariable code: String): ApiResponse<InvitePreviewResult> =
        ApiResponse.ok(crewInviteService.getInvite(code))

    @Operation(
        summary = "처음 온 동승자 참여",
        description = "닉네임만 받아 멤버를 만들고 크루에 넣은 뒤 토큰을 발급합니다. 토큰은 이 응답에서만 받을 수 있습니다. 인증이 필요 없습니다.",
    )
    @SecurityRequirements
    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun joinAsNewMember(
        @PathVariable code: String,
        @Valid @RequestBody request: JoinAsNewMemberRequest,
    ): ApiResponse<InviteJoinResult> =
        ApiResponse.ok(crewInviteService.joinAsNewMember(code, request.nickname))

    @Operation(summary = "기존 멤버 참여", description = "이미 토큰이 있는 멤버를 크루에 넣습니다.")
    @PostMapping("/join")
    fun joinAsMember(
        @LoginMember memberId: Long,
        @PathVariable code: String,
    ): ApiResponse<CrewMemberResult> =
        ApiResponse.ok(crewInviteService.joinAsMember(code, memberId))
}
