package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.LiveTicketResult

// SSE 구독용 일회용 티켓. 브라우저 EventSource는 Authorization 헤더를 붙일 수 없어서
// 토큰으로 티켓을 먼저 받고, 구독 URL에는 짧게 유효한 티켓만 넣음 (토큰이 URL, 로그에 남지 않도록)
interface LiveTicketService {

    fun issue(memberId: Long): LiveTicketResult

    // 한 번 쓰면 사라짐. 없거나 만료됐으면 LIVE_TICKET_INVALID
    fun consume(ticket: String): Long
}
