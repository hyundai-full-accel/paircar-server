package com.fullaccel.paircarserver.trip.api

import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import com.fullaccel.paircarserver.trip.application.TripService
import com.fullaccel.paircarserver.trip.application.dto.TripResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "운행")
@RestController
@RequestMapping("/api/crews/{crewId}/trips")
class CrewTripController(
    private val tripService: TripService,
    private val crewService: CrewService,
) {

    @Operation(
        summary = "진행 중인 운행",
        description = "크루에 아직 끝나지 않은 운행이 있으면 돌려주고, 없으면 data 없이 성공합니다. " +
            "동승자 웹이 처음 열릴 때나 SSE가 끊겼다 다시 붙을 때 지금 상태를 맞추는 데 씁니다.",
    )
    @GetMapping("/current")
    fun getCurrentTrip(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
    ): ApiResponse<TripResult?> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(tripService.getCurrentTrip(crewId))
    }
}
