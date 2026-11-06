package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.PushConfigResult
import com.fullaccel.paircarserver.live.application.dto.PushSubscriptionCommand

// 동승자 웹의 Web Push 구독 관리
interface PushSubscriptionService {

    fun getConfig(): PushConfigResult

    // 같은 endpoint가 있으면 지금 멤버로 갱신함 (같은 브라우저로 다른 멤버가 참여한 경우)
    fun subscribe(memberId: Long, command: PushSubscriptionCommand)

    // 내 구독만 지움. 없으면 아무것도 안 함
    fun unsubscribe(memberId: Long, endpoint: String)
}
