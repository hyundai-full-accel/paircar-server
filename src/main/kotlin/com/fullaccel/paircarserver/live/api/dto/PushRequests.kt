package com.fullaccel.paircarserver.live.api.dto

import com.fullaccel.paircarserver.live.application.dto.PushSubscriptionCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// 브라우저 PushSubscription.toJSON() 결과를 그대로 보내면 됨
data class PushSubscriptionRequest(
    @field:Schema(description = "브라우저 푸시 서비스 주소", example = "https://fcm.googleapis.com/fcm/send/abc...")
    @field:NotBlank
    @field:Size(max = 1024)
    val endpoint: String,

    @field:Valid
    val keys: PushKeysRequest,
) {
    fun toCommand() = PushSubscriptionCommand(endpoint, keys.p256dh, keys.auth)
}

data class PushKeysRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val p256dh: String,

    @field:NotBlank
    @field:Size(max = 255)
    val auth: String,
)

data class PushUnsubscribeRequest(
    @field:NotBlank
    @field:Size(max = 1024)
    val endpoint: String,
)
