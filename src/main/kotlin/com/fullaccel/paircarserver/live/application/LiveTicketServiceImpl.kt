package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.live.application.dto.LiveTicketResult
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

// 티켓은 서버 메모리에만 둠 (서버 1대 기준. 여러 대가 되면 Redis로 옮김)
@Service
class LiveTicketServiceImpl : LiveTicketService {

    private val random = SecureRandom()
    private val tickets = ConcurrentHashMap<String, IssuedTicket>()

    override fun issue(memberId: Long): LiveTicketResult {
        val ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(TICKET_BYTES).also(random::nextBytes))
        val expiresAt = Instant.now().plus(TICKET_LIFETIME)
        tickets[ticket] = IssuedTicket(memberId, expiresAt)
        return LiveTicketResult(ticket, expiresAt)
    }

    override fun consume(ticket: String): Long {
        val issued = tickets.remove(ticket) ?: throw BusinessException(ErrorCode.LIVE_TICKET_INVALID)
        if (issued.expiresAt.isBefore(Instant.now())) throw BusinessException(ErrorCode.LIVE_TICKET_INVALID)
        return issued.memberId
    }

    @Scheduled(fixedRate = 60_000)
    fun removeExpired() {
        val now = Instant.now()
        tickets.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }

    private data class IssuedTicket(val memberId: Long, val expiresAt: Instant)

    companion object {
        private const val TICKET_BYTES = 24
        private val TICKET_LIFETIME: Duration = Duration.ofSeconds(60)
    }
}
