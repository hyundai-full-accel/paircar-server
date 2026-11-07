package com.fullaccel.paircarserver.trip.api

import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import com.fullaccel.paircarserver.trip.application.TripService
import com.fullaccel.paircarserver.trip.application.dto.TripResult
import com.fullaccel.paircarserver.trip.application.dto.TripSummaryResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@Tag(name = "운행")
@RestController
@RequestMapping("/api/crews/{crewId}/trips")
class CrewTripController(
    private val tripService: TripService,
    private val crewService: CrewService,
) {

    @Operation(
        summary = "운행 목록",
        description = "크루의 그 달(운행일, KST) 운행을 시작 순으로 돌려줍니다. 동승자마다 탄 곳, 내린 곳, 탄 시각, 탄 거리가 들어 있습니다. " +
            "동승자 웹 탑승 기록, 차량 앱 운행 기록에 씁니다. 금액은 영수증(boardingId로 맞춤)에 있습니다.",
    )
    @GetMapping
    fun getTripsOfMonth(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
        @Parameter(description = "운행 월", example = "2026-11") @RequestParam month: YearMonth,
    ): ApiResponse<List<TripSummaryResult>> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(tripService.getTripsOfMonth(crewId, month))
    }

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
