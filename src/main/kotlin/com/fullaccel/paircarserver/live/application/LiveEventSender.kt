package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.LiveEvent

// 실시간 이벤트를 멤버에게 보내는 출구. 지금은 SSE 구현 하나 (서버 여러 대가 되면 Redis Pub/Sub 구현으로 교체)
fun interface LiveEventSender {

    // 연결이 없는 멤버는 조용히 건너뜀 (저장하지 않음)
    fun send(memberIds: Collection<Long>, event: LiveEvent)
}
