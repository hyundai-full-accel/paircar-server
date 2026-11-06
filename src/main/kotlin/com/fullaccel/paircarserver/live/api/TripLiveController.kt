package com.fullaccel.paircarserver.live.api

import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import com.fullaccel.paircarserver.live.api.dto.DelayRequest
import com.fullaccel.paircarserver.live.api.dto.VehicleLocationRequest
import com.fullaccel.paircarserver.live.application.LiveReportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "실시간 알림", description = "운행 중 저장하지 않고 상대에게 바로 전달하는 알림. 받는 쪽은 실시간 구독(SSE)으로 받음")
@RestController
@RequestMapping("/api/trips/{tripId}/live")
class TripLiveController(
    private val liveReportService: LiveReportService,
) {

    @Operation(
        summary = "[운전자] 차량 위치, 남은 시간",
        description = "운행 중 5~10초마다 보냅니다. 아직 내리지 않은 동승자들에게 vehicle.location으로 전달합니다.",
    )
    @PostMapping("/location")
    fun reportVehicleLocation(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @Valid @RequestBody request: VehicleLocationRequest,
    ): ApiResponse<Unit> {
        liveReportService.reportVehicleLocation(memberId, tripId, request.toCommand())
        return ApiResponse.ok()
    }

    @Operation(summary = "[운전자] 곧 도착", description = "승차 지점 300m 접근(addProximityAlert) 시 그 동승자에게 vehicle.approaching을 보냅니다.")
    @PostMapping("/riders/{crewMemberId}/approaching")
    fun reportApproaching(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @PathVariable crewMemberId: Long,
    ): ApiResponse<Unit> {
        liveReportService.reportApproaching(memberId, tripId, crewMemberId)
        return ApiResponse.ok()
    }

    @Operation(summary = "[운전자] 차가 도착했어요", description = "경유지에 도착했는데 아직 타지 않은 동승자에게 vehicle.arrived를 보냅니다.")
    @PostMapping("/riders/{crewMemberId}/arrived")
    fun reportArrived(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @PathVariable crewMemberId: Long,
    ): ApiResponse<Unit> {
        liveReportService.reportArrived(memberId, tripId, crewMemberId)
        return ApiResponse.ok()
    }

    @Operation(summary = "[운전자] 늦어요", description = "Gleo \"크루한테 10분 늦는다고 알려줘\". 아직 타지 않은 동승자들에게 driver.delayed를 보냅니다.")
    @PostMapping("/delay")
    fun notifyDriverDelay(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @Valid @RequestBody request: DelayRequest,
    ): ApiResponse<Unit> {
        liveReportService.notifyDriverDelay(memberId, tripId, request.minutes)
        return ApiResponse.ok()
    }

    @Operation(summary = "[동승자] 늦어요", description = "\"5분 늦어요\". 운전자에게 rider.delayed를 보내고, 차량 앱은 TTS로만 알립니다. 경로는 바뀌지 않습니다.")
    @PostMapping("/me/delay")
    fun notifyRiderDelay(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @Valid @RequestBody request: DelayRequest,
    ): ApiResponse<Unit> {
        liveReportService.notifyRiderDelay(memberId, tripId, request.minutes)
        return ApiResponse.ok()
    }

    @Operation(summary = "[동승자] 나와 있어요", description = "운전자에게 rider.here를 보내고, 차량 앱은 TTS로 알립니다.")
    @PostMapping("/me/here")
    fun notifyRiderHere(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
    ): ApiResponse<Unit> {
        liveReportService.notifyRiderHere(memberId, tripId)
        return ApiResponse.ok()
    }
}
