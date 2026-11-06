package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.LiveEvent

// 실시간 이벤트를 채널(SSE, Web Push)로 나눠 보내는 입구. 리스너와 서비스는 이것만 씀
fun interface LiveNotifier {

    fun notify(memberIds: Collection<Long>, event: LiveEvent)
}
