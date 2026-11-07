package com.fullaccel.paircarserver.live.api

import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import com.fullaccel.paircarserver.live.application.LiveTicketService
import com.fullaccel.paircarserver.live.application.dto.LiveTicketResult
import com.fullaccel.paircarserver.live.sse.SseEmitterRegistry
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(
    name = "실시간 구독",
    description = "SSE로 내 실시간 이벤트를 받습니다. 1) 토큰으로 티켓 발급 2) 60초 안에 티켓으로 구독. " +
        "SSE event 이름이 이벤트 종류이고, data는 { type, tripId, data, occurredAt } JSON입니다. 끊기면 새 티켓으로 다시 구독하세요.",
)
@RestController
@RequestMapping("/api/live")
class LiveController(
    private val liveTicketService: LiveTicketService,
    private val sseEmitterRegistry: SseEmitterRegistry,
) {

    @Operation(summary = "구독 티켓 발급", description = "60초 동안 한 번만 쓸 수 있습니다. 토큰을 구독 URL에 넣지 않기 위한 장치입니다.")
    @PostMapping("/tickets")
    fun issueTicket(@LoginMember memberId: Long): ApiResponse<LiveTicketResult> =
        ApiResponse.ok(liveTicketService.issue(memberId))

    @Operation(
        summary = "실시간 이벤트 구독 (SSE)",
        description = "연결되면 먼저 connected 이벤트가 옵니다. 25초마다 keepalive 주석이 옵니다. " +
            "동승자: trip.started, vehicle.location, vehicle.approaching, vehicle.arrived, rider.boarded, driver.delayed, trip.ended. " +
            "운전자: rider.delayed, rider.here, rider.absent, rider.place-changed. " +
            "크루 멤버 모두: rider.attendance-changed (출발 전 출석 변경, data에 serviceDate), detour.confirm-requested.",
    )
    @SecurityRequirements
    @GetMapping("/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(@RequestParam ticket: String): SseEmitter =
        sseEmitterRegistry.connect(liveTicketService.consume(ticket))
}
