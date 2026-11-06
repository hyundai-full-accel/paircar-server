package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.PushConfigResult
import com.fullaccel.paircarserver.live.application.dto.PushSubscriptionCommand
import com.fullaccel.paircarserver.live.domain.PushSubscription
import com.fullaccel.paircarserver.live.domain.PushSubscriptionRepository
import com.fullaccel.paircarserver.live.push.WebPushProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PushSubscriptionServiceImpl(
    private val pushSubscriptionRepository: PushSubscriptionRepository,
    private val webPushProperties: WebPushProperties,
) : PushSubscriptionService {

    override fun getConfig() = PushConfigResult(
        enabled = webPushProperties.enabled,
        publicKey = webPushProperties.publicKey.takeIf { webPushProperties.enabled },
    )

    @Transactional
    override fun subscribe(memberId: Long, command: PushSubscriptionCommand) {
        val existing = pushSubscriptionRepository.findByEndpoint(command.endpoint)
        if (existing != null) {
            existing.renew(memberId, command.p256dh, command.auth)
            return
        }
        pushSubscriptionRepository.save(
            PushSubscription(memberId = memberId, endpoint = command.endpoint, p256dh = command.p256dh, auth = command.auth),
        )
    }

    @Transactional
    override fun unsubscribe(memberId: Long, endpoint: String) {
        val subscription = pushSubscriptionRepository.findByEndpoint(endpoint) ?: return
        if (subscription.memberId == memberId) pushSubscriptionRepository.delete(subscription)
    }
}
