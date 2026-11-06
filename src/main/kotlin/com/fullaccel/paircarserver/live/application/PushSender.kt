package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.PushMessage

// 웹을 닫아도 받는 폰 알림의 출구. 구독이 없는 멤버는 건너뜀
fun interface PushSender {

    fun send(memberIds: Collection<Long>, message: PushMessage)
}
