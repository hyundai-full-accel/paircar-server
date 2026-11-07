package com.fullaccel.paircarserver.crew.api

import com.fullaccel.paircarserver.crew.api.dto.RegisterDetourRequest
import com.fullaccel.paircarserver.crew.application.CrewDetourService
import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.crew.application.dto.DetourResult
import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "우회 거리", description = "운전자가 동승자를 태우거나 내려 주느라 더 달리는 거리 (기술명세 T4). 운전자, 동승자가 둘 다 확인해야 정산에 씀")
@RestController
@RequestMapping("/api/crews/{crewId}/detours")
class CrewDetourController(
    private val crewService: CrewService,
    private val crewDetourService: CrewDetourService,
) {

    @Operation(
        summary = "우회 거리 등록",
        description = "차량 앱이 경유 경로와 직행 경로를 비교해 계산한 값을 올립니다. 같은 동승자, 운전자, 장소가 있으면 값을 바꾸고, 값이 달라지면 양측 확인을 초기화합니다. " +
            "placeId는 운행 시작 때 그 동승자의 boardPlaceId(또는 alightPlaceId)와 같은 장소여야 정산에 들어갑니다. 출발지, 목적지에 등록한 값은 쓰지 않습니다.",
    )
    @PutMapping
    fun registerDetour(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
        @Valid @RequestBody request: RegisterDetourRequest,
    ): ApiResponse<DetourResult> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(crewDetourService.registerDetour(crewId, request.toCommand()))
    }

    @Operation(summary = "우회 거리 목록")
    @GetMapping
    fun getDetours(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
    ): ApiResponse<List<DetourResult>> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(crewDetourService.getDetours(crewId))
    }

    @Operation(summary = "우회 거리 확인", description = "요청한 사람이 그 우회 거리의 운전자나 동승자여야 합니다. 둘 다 확인하면 confirmed가 true가 됩니다.")
    @PostMapping("/{detourId}/confirm")
    fun confirmDetour(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
        @PathVariable detourId: Long,
    ): ApiResponse<DetourResult> {
        val crewMemberId = crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(crewDetourService.confirmDetour(detourId, crewMemberId))
    }
}
