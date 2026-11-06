package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import org.springframework.stereotype.Component

// 모든 이벤트는 SSE로 보내고, 놓치면 안 되는 이벤트는 Web Push로도 보냄 (웹을 닫아도 받도록)
@Component
class DefaultLiveNotifier(
    private val liveEventSender: LiveEventSender,
    private val pushSender: PushSender,
) : LiveNotifier {

    override fun notify(memberIds: Collection<Long>, event: LiveEvent) {
        if (memberIds.isEmpty()) return
        liveEventSender.send(memberIds, event)
        PushMessages.of(event)?.let { pushSender.send(memberIds, it) }
    }
}
