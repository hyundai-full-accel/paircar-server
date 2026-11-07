package com.fullaccel.paircarserver.crew.api

import com.fullaccel.paircarserver.crew.api.dto.AttendanceRequest
import com.fullaccel.paircarserver.crew.api.dto.ChangeMemberPlacesRequest
import com.fullaccel.paircarserver.crew.application.CrewAttendanceService
import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.crew.application.CrewSettingService
import com.fullaccel.paircarserver.crew.application.dto.AttendanceResult
import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(
    name = "탈 곳, 출석",
    description = "동승자의 평소 탈 곳, 내릴 곳과 그날 출석. 차량 앱은 출발 전에 출석 목록으로 오늘 태울 사람과 경유지를 정함. " +
        "본인이나 차를 등록한 크루 멤버만 바꿀 수 있음",
)
@RestController
@RequestMapping("/api/crews/{crewId}")
class CrewMemberController(
    private val crewService: CrewService,
    private val crewSettingService: CrewSettingService,
    private val crewAttendanceService: CrewAttendanceService,
) {

    @Operation(summary = "평소 탈 곳, 내릴 곳, 픽업 시각", description = "바꿀 수 있는 값 전체를 보내고 null은 지웁니다. 장소는 이 크루 것이어야 합니다.")
    @PutMapping("/members/{crewMemberId}/places")
    fun changeMemberPlaces(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
        @PathVariable crewMemberId: Long,
        @Valid @RequestBody request: ChangeMemberPlacesRequest,
    ): ApiResponse<CrewMemberResult> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(
            crewSettingService.changeMemberPlaces(
                memberId,
                crewId,
                crewMemberId,
                request.boardPlaceId,
                request.alightPlaceId,
                request.pickupTime,
            ),
        )
    }

    @Operation(
        summary = "정산 기준 동의",
        description = "지금 크루의 정산 방식(나누는 방법, 운전자 포함)에 동의합니다. 본인만 할 수 있습니다. " +
            "동의 뒤 정산 방식이 바뀌면 크루원 목록의 settlementAgreed가 false가 되어 다시 동의를 받습니다.",
    )
    @PostMapping("/members/{crewMemberId}/settlement-agreement")
    fun agreeSettlement(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
        @PathVariable crewMemberId: Long,
    ): ApiResponse<CrewMemberResult> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(crewSettingService.agreeSettlement(memberId, crewId, crewMemberId))
    }

    @Operation(
        summary = "그날 출석 목록",
        description = "크루 멤버 전원의 그날 출석입니다. 따로 정하지 않은 사람은 탐(attending: true), 평소 탈 곳으로 나옵니다.",
    )
    @GetMapping("/attendance/{date}")
    fun getAttendances(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
        @Parameter(description = "운행일 (KST)", example = "2026-11-09") @PathVariable date: LocalDate,
    ): ApiResponse<List<AttendanceResult>> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(crewAttendanceService.getAttendances(crewId, date))
    }

    @Operation(
        summary = "그날 출석 바꾸기",
        description = "\"오늘은 안 타요\", \"오늘만 후문에서 탈게요\". 오늘(KST) 이후 날짜만 바꿀 수 있습니다. " +
            "바뀌면 바꾼 사람을 뺀 크루 멤버에게 rider.attendance-changed를 보냅니다. " +
            "이미 시작한 운행에는 영향이 없습니다 (운행 중 불참은 운행 API의 불참 처리).",
    )
    @PutMapping("/members/{crewMemberId}/attendance/{date}")
    fun changeAttendance(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
        @PathVariable crewMemberId: Long,
        @Parameter(description = "운행일 (KST)", example = "2026-11-09") @PathVariable date: LocalDate,
        @Valid @RequestBody request: AttendanceRequest,
    ): ApiResponse<AttendanceResult> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(crewAttendanceService.changeAttendance(memberId, crewId, crewMemberId, date, request.toCommand()))
    }
}
