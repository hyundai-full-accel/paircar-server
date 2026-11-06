package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.DriverData
import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import com.fullaccel.paircarserver.live.application.dto.LiveEventType
import com.fullaccel.paircarserver.trip.application.event.BoardPlaceChangedEvent
import com.fullaccel.paircarserver.trip.application.event.BoardingCanceledEvent
import com.fullaccel.paircarserver.trip.application.event.TripStartedEvent
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TripLiveEventListenerTest {

    private val sent = mutableListOf<Pair<Collection<Long>, LiveEvent>>()
    private val listener = TripLiveEventListener { memberIds, event -> sent += memberIds to event }

    @Test
    fun `운행 시작은 동승자들에게 trip_started로 보낸다`() {
        listener.on(TripStartedEvent(tripId = 1, driverMemberId = 10, driverNickname = "지훈", riderMemberIds = listOf(20, 30)))

        val (recipients, event) = sent.single()
        assertEquals(listOf(20L, 30L), recipients)
        assertEquals(LiveEventType.TRIP_STARTED, event.type)
        assertEquals(DriverData("지훈"), event.data)
    }

    @Test
    fun `동승자가 직접 불참하면 운전자에게 rider_absent로 보낸다`() {
        listener.on(BoardingCanceledEvent(tripId = 1, driverMemberId = 10, riderCrewMemberId = 2, riderNickname = "수아", byRider = true))

        val (recipients, event) = sent.single()
        assertEquals(listOf(10L), recipients)
        assertEquals(LiveEventType.RIDER_ABSENT, event.type)
    }

    @Test
    fun `운전자가 직접 바꾼 불참, 탈 지점 변경은 보내지 않는다`() {
        listener.on(BoardingCanceledEvent(tripId = 1, driverMemberId = 10, riderCrewMemberId = 2, riderNickname = "수아", byRider = false))
        listener.on(
            BoardPlaceChangedEvent(
                tripId = 1, driverMemberId = 10, riderCrewMemberId = 2, riderNickname = "수아",
                placeId = 5, placeName = "후문", latitude = BigDecimal("37.4"), longitude = BigDecimal("127.1"), byRider = false,
            ),
        )

        assertTrue(sent.isEmpty())
    }
}
