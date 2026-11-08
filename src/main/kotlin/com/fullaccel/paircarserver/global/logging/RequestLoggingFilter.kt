package com.fullaccel.paircarserver.global.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

// 요청마다 requestId를 정해 로그 문맥(MDC)에 넣고, 끝나면 한 줄로 남김
//   INFO [req=8f3a2c1d4e5b6a70 m=12] POST /api/trips/3/end → 200 (34ms)
// requestId는 nginx가 넘긴 X-Request-Id를 쓰고(없으면 새로 만듦) 응답 헤더로도 돌려줌. 앱이 오류를 보고할 때 이 값으로 서버 로그를 찾음
// 요청 바디와 토큰은 남기지 않음. SSE 구독 URL의 ticket은 가림
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // CORS 등 다른 필터보다 먼저: 거기서 끝난 요청도 남김
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    // 헬스체크는 몇 초마다 와서 로그를 덮으므로 뺌
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.requestURI.startsWith("/actuator")

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val requestId = request.getHeader(HEADER)?.takeIf { VALID_ID.matches(it) } ?: newRequestId()
        MDC.put(LogContext.REQUEST_ID, requestId)
        response.setHeader(HEADER, requestId)
        val startedAt = System.nanoTime()
        try {
            chain.doFilter(request, response)
        } finally {
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
            val target = request.method + " " + request.requestURI + maskedQuery(request.queryString)
            if (request.isAsyncStarted) {
                log.info("{} → 스트림 시작 ({}ms)", target, elapsedMs) // SSE 구독: 연결은 계속 열려 있음
            } else {
                log.info("{} → {} ({}ms)", target, response.status, elapsedMs)
            }
            MDC.clear()
        }
    }

    private fun maskedQuery(query: String?): String =
        if (query == null) "" else "?" + query.replace(TICKET_PARAM, "$1***")

    companion object {
        const val HEADER = "X-Request-Id"
        private val VALID_ID = Regex("[A-Za-z0-9._-]{1,64}")
        private val TICKET_PARAM = Regex("(ticket=)[^&]*")

        private fun newRequestId(): String = UUID.randomUUID().toString().replace("-", "").take(16)
    }
}
