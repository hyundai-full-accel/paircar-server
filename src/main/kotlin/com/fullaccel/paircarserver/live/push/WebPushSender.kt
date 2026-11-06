package com.fullaccel.paircarserver.live.push

import com.fasterxml.jackson.databind.ObjectMapper
import com.fullaccel.paircarserver.live.application.PushSender
import com.fullaccel.paircarserver.live.application.dto.PushMessage
import com.fullaccel.paircarserver.live.application.dto.PushUrgency
import com.fullaccel.paircarserver.live.domain.PushSubscription
import com.fullaccel.paircarserver.live.domain.PushSubscriptionRepository
import nl.martijndwars.webpush.Encoding
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Urgency
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.security.Security

// VAPID Web Push 발송. 푸시 서비스 왕복이 있어 요청 스레드를 붙잡지 않도록 비동기로 보냄
// 404, 410은 브라우저가 구독을 없앤 것이므로 그 구독을 지움
@Component
class WebPushSender(
    private val properties: WebPushProperties,
    private val pushSubscriptionRepository: PushSubscriptionRepository,
    private val objectMapper: ObjectMapper,
) : PushSender {

    private val log = LoggerFactory.getLogger(javaClass)

    private val pushService: PushService? by lazy {
        if (!properties.enabled) return@lazy null
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(BouncyCastleProvider())
        PushService(properties.publicKey, properties.privateKey, properties.subject)
    }

    @Async
    override fun send(memberIds: Collection<Long>, message: PushMessage) {
        val service = pushService ?: return
        if (memberIds.isEmpty()) return
        val payload = objectMapper.writeValueAsBytes(
            mapOf("type" to message.type, "title" to message.title, "body" to message.body, "url" to message.url),
        )
        pushSubscriptionRepository.findAllByMemberIdIn(memberIds.toSet()).forEach { subscription ->
            sendTo(service, subscription, payload, message)
        }
    }

    private fun sendTo(service: PushService, subscription: PushSubscription, payload: ByteArray, message: PushMessage) {
        try {
            val notification = Notification.builder()
                .endpoint(subscription.endpoint)
                .userPublicKey(subscription.p256dh)
                .userAuth(subscription.auth)
                .payload(payload)
                .ttl(message.ttlSeconds)
                .urgency(if (message.urgency == PushUrgency.HIGH) Urgency.HIGH else Urgency.NORMAL)
                .build()
            val status = service.send(notification, Encoding.AES128GCM).statusLine.statusCode
            when (status) {
                in 200..299 -> Unit
                404, 410 -> pushSubscriptionRepository.deleteByEndpoint(subscription.endpoint)
                else -> log.warn("Web Push rejected: status {} for member {}", status, subscription.memberId)
            }
        } catch (e: Exception) {
            log.warn("Web Push failed for member {}", subscription.memberId, e)
        }
    }
}
