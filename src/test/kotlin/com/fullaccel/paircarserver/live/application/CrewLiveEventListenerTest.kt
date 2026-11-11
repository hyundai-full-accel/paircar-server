package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.crew.application.event.AttendanceChangedEvent
import com.fullaccel.paircarserver.crew.application.event.AttendancePlace
import com.fullaccel.paircarserver.crew.application.event.CrewMemberJoinedEvent
import com.fullaccel.paircarserver.crew.application.event.CrewMemberPlacesChangedEvent
import com.fullaccel.paircarserver.live.application.dto.AttendanceData
import com.fullaccel.paircarserver.live.application.dto.CrewMemberData
import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import com.fullaccel.paircarserver.live.application.dto.LiveEventType
import com.fullaccel.paircarserver.live.application.dto.PlaceData
import com.fullaccel.paircarserver.live.application.dto.UsualPlacesData
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
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
                attending = true, boardPlace = place, delayMinutes = 10, recipientMemberIds = listOf(10, 30),
            ),
        )

        val (recipients, event) = sent.single()
        assertEquals(listOf(10L, 30L), recipients)
        assertEquals(LiveEventType.RIDER_ATTENDANCE_CHANGED, event.type)
        assertNull(event.tripId) // 출발 전이라 운행이 없음
        assertEquals(
            AttendanceData(1, 2, "수아", LocalDate.of(2026, 11, 9), true, PlaceData(5, "후문", BigDecimal("37.4"), BigDecimal("127.1")), 10),
            event.data,
        )
    }

    @Test
    fun `새 멤버 참여는 받을 사람에게 crew_member-joined로 보낸다`() {
        listener.on(CrewMemberJoinedEvent(crewId = 1, crewMemberId = 2, nickname = "수아", recipientMemberIds = listOf(10)))

        val (recipients, event) = sent.single()
        assertEquals(listOf(10L), recipients)
        assertEquals(LiveEventType.CREW_MEMBER_JOINED, event.type)
        assertEquals(CrewMemberData(1, 2, "수아"), event.data)
    }

    @Test
    fun `평소 탈 곳 변경은 받을 사람에게 rider_usual-places-changed로 보낸다`() {
        val gate = AttendancePlace(5, "정문", BigDecimal("37.4"), BigDecimal("127.1"))
        listener.on(
            CrewMemberPlacesChangedEvent(
                crewId = 1, crewMemberId = 2, nickname = "수아", boardPlace = gate, alightPlace = null,
                pickupTime = LocalTime.of(7, 50), recipientMemberIds = listOf(10),
            ),
        )

        val (recipients, event) = sent.single()
        assertEquals(listOf(10L), recipients)
        assertEquals(LiveEventType.RIDER_USUAL_PLACES_CHANGED, event.type)
        assertEquals(
            UsualPlacesData(1, 2, "수아", PlaceData(5, "정문", BigDecimal("37.4"), BigDecimal("127.1")), null, LocalTime.of(7, 50)),
            event.data,
        )
    }
}
