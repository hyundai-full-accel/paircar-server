package com.fullaccel.paircarserver.crew.api

import com.fullaccel.paircarserver.crew.api.dto.CreateCrewRequest
import com.fullaccel.paircarserver.crew.application.CrewInviteService
import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.crew.application.dto.CrewResult
import com.fullaccel.paircarserver.crew.application.dto.InviteResult
import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
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

@Tag(name = "크루", description = "크루, 크루 멤버, 초대 코드 발급. 크루 API는 그 크루 멤버만 호출할 수 있음")
@RestController
@RequestMapping("/api/crews")
class CrewController(
    private val crewService: CrewService,
    private val crewInviteService: CrewInviteService,
) {

    @Operation(summary = "크루 생성", description = "만든 사람은 자동으로 크루에 참여합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCrew(
        @LoginMember memberId: Long,
        @Valid @RequestBody request: CreateCrewRequest,
    ): ApiResponse<CrewResult> =
        ApiResponse.ok(crewService.createCrew(memberId, request.name))

    @Operation(summary = "내 크루 목록")
    @GetMapping("/me")
    fun getMyCrews(@LoginMember memberId: Long): ApiResponse<List<CrewResult>> =
        ApiResponse.ok(crewService.getCrewsOfMember(memberId))

    @Operation(summary = "크루 조회")
    @GetMapping("/{crewId}")
    fun getCrew(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
    ): ApiResponse<CrewResult> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(crewService.getCrew(crewId))
    }

    @Operation(summary = "크루 멤버 목록", description = "참여 순서대로 반환합니다. crewMemberId로 운행, 우회 거리 API를 호출합니다.")
    @GetMapping("/{crewId}/members")
    fun getCrewMembers(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
    ): ApiResponse<List<CrewMemberResult>> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(crewService.getCrewMembers(crewId))
    }

    @Operation(summary = "초대 코드 발급", description = "7일 동안 유효합니다. 동승자에게 code가 들어간 링크를 보내세요.")
    @PostMapping("/{crewId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    fun createInvite(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
    ): ApiResponse<InviteResult> =
        ApiResponse.ok(crewInviteService.createInvite(crewId, memberId))
}
