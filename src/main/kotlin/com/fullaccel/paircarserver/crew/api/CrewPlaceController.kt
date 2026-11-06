package com.fullaccel.paircarserver.crew.api

import com.fullaccel.paircarserver.crew.api.dto.RegisterPlaceRequest
import com.fullaccel.paircarserver.crew.application.CrewPlaceService
import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.crew.application.dto.CrewPlaceResult
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

@Tag(name = "크루 장소", description = "아파트 정문, 회사, 운전자 집. 운행의 출발지, 목적지, 탈 지점, 내릴 지점으로 씀")
@RestController
@RequestMapping("/api/crews/{crewId}/places")
class CrewPlaceController(
    private val crewService: CrewService,
    private val crewPlaceService: CrewPlaceService,
) {

    @Operation(summary = "장소 등록", description = "같은 크루에 같은 이름이 있으면 CREW_PLACE_NAME_DUPLICATED입니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registerPlace(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
        @Valid @RequestBody request: RegisterPlaceRequest,
    ): ApiResponse<CrewPlaceResult> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(crewPlaceService.registerPlace(crewId, request.toCommand()))
    }

    @Operation(summary = "장소 목록")
    @GetMapping
    fun getPlaces(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
    ): ApiResponse<List<CrewPlaceResult>> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(crewPlaceService.getPlaces(crewId))
    }
}
