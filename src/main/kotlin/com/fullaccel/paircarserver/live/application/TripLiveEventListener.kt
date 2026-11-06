package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.DriverData
import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import com.fullaccel.paircarserver.live.application.dto.LiveEventType
import com.fullaccel.paircarserver.live.application.dto.RiderData
import com.fullaccel.paircarserver.live.application.dto.RiderPlaceData
import com.fullaccel.paircarserver.trip.application.event.BoardPlaceChangedEvent
import com.fullaccel.paircarserver.trip.application.event.BoardingCanceledEvent
import com.fullaccel.paircarserver.trip.application.event.RiderBoardedEvent
import com.fullaccel.paircarserver.trip.application.event.TripEndedEvent
import com.fullaccel.paircarserver.trip.application.event.TripStartedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

// 운행 모듈의 도메인 이벤트를 받아 실시간 이벤트로 바꿔 보냄
// 커밋된 변경만 알리도록 트랜잭션 커밋 뒤에 실행됨 (롤백되면 알리지 않음)
@Component
class TripLiveEventListener(
    private val liveNotifier: LiveNotifier,
) {

    @TransactionalEventListener
    fun on(event: TripStartedEvent) {
        liveNotifier.notify(event.riderMemberIds, LiveEvent(LiveEventType.TRIP_STARTED, event.tripId, DriverData(event.driverNickname)))
    }

    @TransactionalEventListener
    fun on(event: RiderBoardedEvent) {
        liveNotifier.notify(
            listOf(event.riderMemberId),
            LiveEvent(LiveEventType.RIDER_BOARDED, event.tripId, RiderData(event.riderCrewMemberId, event.riderNickname)),
        )
    }

    @TransactionalEventListener
    fun on(event: TripEndedEvent) {
        liveNotifier.notify(event.riderMemberIds, LiveEvent(LiveEventType.TRIP_ENDED, event.tripId))
    }

    // 운전자가 직접 바꾼 경우는 차량 앱이 이미 알고 있으므로 알리지 않음
    @TransactionalEventListener
    fun on(event: BoardingCanceledEvent) {
        if (!event.byRider) return
        liveNotifier.notify(
            listOf(event.driverMemberId),
            LiveEvent(LiveEventType.RIDER_ABSENT, event.tripId, RiderData(event.riderCrewMemberId, event.riderNickname)),
        )
    }

    @TransactionalEventListener
    fun on(event: BoardPlaceChangedEvent) {
        if (!event.byRider) return
        val data = RiderPlaceData(
            crewMemberId = event.riderCrewMemberId,
            nickname = event.riderNickname,
            placeId = event.placeId,
            placeName = event.placeName,
            latitude = event.latitude,
            longitude = event.longitude,
        )
        liveNotifier.notify(listOf(event.driverMemberId), LiveEvent(LiveEventType.RIDER_PLACE_CHANGED, event.tripId, data))
    }
}
