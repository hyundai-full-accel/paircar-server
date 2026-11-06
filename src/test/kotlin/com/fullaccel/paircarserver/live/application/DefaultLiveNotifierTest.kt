package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import com.fullaccel.paircarserver.live.application.dto.LiveEventType
import com.fullaccel.paircarserver.live.application.dto.PushMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultLiveNotifierTest {

    private val sse = mutableListOf<LiveEvent>()
    private val push = mutableListOf<PushMessage>()
    private val notifier = DefaultLiveNotifier({ _, event -> sse += event }, { _, message -> push += message })

    @Test
    fun `놓치면 안 되는 이벤트는 SSE와 Push 둘 다 보낸다`() {
        notifier.notify(listOf(1), LiveEvent(LiveEventType.VEHICLE_APPROACHING, 7))

        assertEquals(1, sse.size)
        assertEquals("vehicle.approaching", push.single().type)
    }

    @Test
    fun `나머지 이벤트는 SSE로만 보낸다`() {
        notifier.notify(listOf(1), LiveEvent(LiveEventType.TRIP_ENDED, 7))

        assertEquals(1, sse.size)
        assertTrue(push.isEmpty())
    }

    @Test
    fun `받는 사람이 없으면 아무것도 보내지 않는다`() {
        notifier.notify(emptyList(), LiveEvent(LiveEventType.VEHICLE_APPROACHING, 7))

        assertTrue(sse.isEmpty() && push.isEmpty())
    }
}
