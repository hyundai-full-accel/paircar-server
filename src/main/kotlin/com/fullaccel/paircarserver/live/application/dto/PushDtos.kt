package com.fullaccel.paircarserver.live.application.dto

// 브라우저 PushSubscription.toJSON()과 같은 모양
data class PushSubscriptionCommand(
    val endpoint: String,
    val p256dh: String,
    val auth: String,
)

data class PushConfigResult(
    val enabled: Boolean,
    val publicKey: String?, // 브라우저 subscribe의 applicationServerKey
)

// 폰 알림으로 보여 줄 내용. 서비스 워커는 title, body를 그대로 띄우고 누르면 url을 엶
data class PushMessage(
    val type: String,
    val title: String,
    val body: String,
    val url: String,
    val ttlSeconds: Int,
    val urgency: PushUrgency,
)

enum class PushUrgency {
    NORMAL,
    HIGH, // 지금 바로 봐야 하는 알림 (곧 도착, 차 도착)
}
