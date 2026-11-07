package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.crew.application.event.AttendanceChangedEvent
import com.fullaccel.paircarserver.crew.application.event.AttendancePlace
import com.fullaccel.paircarserver.live.application.dto.AttendanceData
import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import com.fullaccel.paircarserver.live.application.dto.LiveEventType
import com.fullaccel.paircarserver.live.application.dto.PlaceData
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CrewLiveEventListenerTest {

    private val sent = mutableListOf<Pair<Collection<Long>, LiveEvent>>()
    private val listener = CrewLiveEventListener { memberIds, event -> sent += memberIds to event }

    @Test
    fun `출석 변경은 다른 크루 멤버에게 날짜, 탈 곳과 함께 rider_attendance-changed로 보낸다`() {
        val place = AttendancePlace(5, "후문", BigDecimal("37.4"), BigDecimal("127.1"))
        listener.on(
            AttendanceChangedEvent(
                crewId = 1, crewMemberId = 2, nickname = "수아", serviceDate = LocalDate.of(2026, 11, 9),
                attending = true, boardPlace = place, recipientMemberIds = listOf(10, 30),
            ),
        )

        val (recipients, event) = sent.single()
        assertEquals(listOf(10L, 30L), recipients)
        assertEquals(LiveEventType.RIDER_ATTENDANCE_CHANGED, event.type)
        assertNull(event.tripId) // 출발 전이라 운행이 없음
        assertEquals(
            AttendanceData(1, 2, "수아", LocalDate.of(2026, 11, 9), true, PlaceData(5, "후문", BigDecimal("37.4"), BigDecimal("127.1"))),
            event.data,
        )
    }
}
