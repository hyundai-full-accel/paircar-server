package com.fullaccel.paircarserver.global.logging

import org.slf4j.MDC

// 로그 줄마다 붙는 문맥 (application.yaml logging.pattern.level의 [req=… m=…])
// requestId: 요청 하나를 끝까지 따라가는 ID. 스케줄 작업이면 "job:이름"
// memberId: 토큰으로 확인한 멤버 (@LoginMember)
object LogContext {
    const val REQUEST_ID = "requestId"
    const val MEMBER_ID = "memberId"

    fun putMember(memberId: Long) {
        MDC.put(MEMBER_ID, memberId.toString())
    }

    // 요청 밖에서 도는 작업(월 자동 정산 등)의 로그에 작업 이름을 붙임
    inline fun <T> job(name: String, block: () -> T): T {
        MDC.put(REQUEST_ID, "job:$name")
        try {
            return block()
        } finally {
            MDC.clear()
        }
    }
}
