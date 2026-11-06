package com.fullaccel.paircarserver.live.push

import org.springframework.boot.context.properties.ConfigurationProperties

// VAPID 키 설정. 키가 비어 있으면 Push를 보내지 않음
@ConfigurationProperties("paircar.push")
data class WebPushProperties(
    val publicKey: String = "",
    val privateKey: String = "",
    val subject: String = "mailto:admin@paircar.app",
) {
    val enabled: Boolean
        get() = publicKey.isNotBlank() && privateKey.isNotBlank()
}
