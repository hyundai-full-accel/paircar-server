package com.fullaccel.paircarserver.live.sse

import com.fullaccel.paircarserver.live.application.LiveEventSender
import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

// 멤버별 SSE 연결 보관소. 한 멤버가 기기, 탭을 여러 개 열 수 있어 멤버당 연결 목록을 둠
// 서버 메모리에만 있으므로 서버가 1대일 때만 동작함
@Component
class SseEmitterRegistry : LiveEventSender {

    private val log = LoggerFactory.getLogger(javaClass)
    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    fun connect(memberId: Long): SseEmitter {
        val emitter = SseEmitter(TIMEOUT.toMillis())
        emitters.computeIfAbsent(memberId) { CopyOnWriteArrayList() }.add(emitter)
        emitter.onCompletion { remove(memberId, emitter) }
        emitter.onTimeout { remove(memberId, emitter) }
        emitter.onError { remove(memberId, emitter) }
        trySend(memberId, emitter, SseEmitter.event().name(CONNECTED_EVENT).data(CONNECTED_EVENT))
        return emitter
    }

    override fun send(memberIds: Collection<Long>, event: LiveEvent) {
        memberIds.toSet().forEach { memberId ->
            emitters[memberId]?.forEach { emitter ->
                trySend(memberId, emitter, SseEmitter.event().name(event.type.value).data(event))
            }
        }
    }

    fun connectionCount(memberId: Long): Int = emitters[memberId]?.size ?: 0

    // 프록시, 로드밸런서가 유휴 연결을 끊지 않도록 주기적으로 주석 한 줄을 보냄
    @Scheduled(fixedRateString = "\${paircar.live.heartbeat-interval:25s}")
    fun sendHeartbeat() {
        emitters.forEach { (memberId, list) ->
            list.forEach { trySend(memberId, it, SseEmitter.event().comment(HEARTBEAT_COMMENT)) }
        }
    }

    private fun trySend(memberId: Long, emitter: SseEmitter, event: SseEmitter.SseEventBuilder) {
        try {
            emitter.send(event)
        } catch (e: IOException) {
            log.debug("SSE send failed, dropping connection of member {}", memberId)
            remove(memberId, emitter)
        } catch (e: IllegalStateException) {
            remove(memberId, emitter) // 이미 완료된 연결
        }
    }

    private fun remove(memberId: Long, emitter: SseEmitter) {
        emitters.computeIfPresent(memberId) { _, list ->
            list.remove(emitter)
            list.ifEmpty { null }
        }
    }

    companion object {
        private val TIMEOUT: Duration = Duration.ofMinutes(30) // 끊기면 클라이언트가 다시 연결함
        private const val CONNECTED_EVENT = "connected"
        private const val HEARTBEAT_COMMENT = "keepalive"
    }
}
