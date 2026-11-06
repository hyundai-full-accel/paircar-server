package com.fullaccel.paircarserver.crew.application

import org.springframework.boot.context.properties.ConfigurationProperties

// 초대 링크 설정. baseUrl은 동승자 웹의 참여 경로 (환경변수 PAIRCAR_INVITE_BASE_URL)
@ConfigurationProperties("paircar.invite")
data class InviteProperties(
    val baseUrl: String,
) {
    fun inviteUrl(code: String): String = "${baseUrl.trimEnd('/')}/$code"
}
