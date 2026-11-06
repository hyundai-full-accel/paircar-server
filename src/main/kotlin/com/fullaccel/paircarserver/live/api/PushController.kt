package com.fullaccel.paircarserver.live.api

import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import com.fullaccel.paircarserver.live.api.dto.PushSubscriptionRequest
import com.fullaccel.paircarserver.live.api.dto.PushUnsubscribeRequest
import com.fullaccel.paircarserver.live.application.PushSubscriptionService
import com.fullaccel.paircarserver.live.application.dto.PushConfigResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Web Push",
    description = "동승자 웹이 닫혀 있어도 받는 폰 알림. 출발, 곧 도착, 차 도착, 운전자 지각, 우회 거리 확인 요청, 영수증 도착. " +
        "iOS는 동승자 웹을 홈 화면에 추가해야 받을 수 있음",
)
@RestController
@RequestMapping("/api/push")
class PushController(
    private val pushSubscriptionService: PushSubscriptionService,
) {

    @Operation(
        summary = "Push 설정",
        description = "publicKey를 브라우저 pushManager.subscribe의 applicationServerKey로 쓰세요. enabled가 false면 서버에 VAPID 키가 없어 Push를 보내지 않습니다.",
    )
    @SecurityRequirements
    @GetMapping("/config")
    fun getConfig(): ApiResponse<PushConfigResult> = ApiResponse.ok(pushSubscriptionService.getConfig())

    @Operation(summary = "Push 구독 등록", description = "브라우저 PushSubscription.toJSON() 결과를 그대로 보내세요. 같은 endpoint면 갱신합니다.")
    @PutMapping("/subscriptions")
    fun subscribe(
        @LoginMember memberId: Long,
        @Valid @RequestBody request: PushSubscriptionRequest,
    ): ApiResponse<Unit> {
        pushSubscriptionService.subscribe(memberId, request.toCommand())
        return ApiResponse.ok()
    }

    @Operation(summary = "Push 구독 해제", description = "내 구독만 지웁니다.")
    @DeleteMapping("/subscriptions")
    fun unsubscribe(
        @LoginMember memberId: Long,
        @Valid @RequestBody request: PushUnsubscribeRequest,
    ): ApiResponse<Unit> {
        pushSubscriptionService.unsubscribe(memberId, request.endpoint)
        return ApiResponse.ok()
    }
}
