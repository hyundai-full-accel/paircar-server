package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.crew.application.event.AttendanceChangedEvent
import com.fullaccel.paircarserver.crew.application.event.DetourConfirmationRequestedEvent
import com.fullaccel.paircarserver.live.application.dto.AttendanceData
import com.fullaccel.paircarserver.live.application.dto.DetourData
import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import com.fullaccel.paircarserver.live.application.dto.LiveEventType
import com.fullaccel.paircarserver.live.application.dto.PlaceData
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

// 크루 모듈의 도메인 이벤트를 실시간 알림으로 보냄 (커밋 뒤)
@Component
class CrewLiveEventListener(
    private val liveNotifier: LiveNotifier,
) {

    // 운전자, 동승자 둘 다 확인해야 하므로 둘 다에게 보냄
    @TransactionalEventListener
    fun on(event: DetourConfirmationRequestedEvent) {
        val data = DetourData(
            detourId = event.detourId,
            crewId = event.crewId,
            placeName = event.placeName,
            detourKm = event.detourKm,
            driverNickname = event.driverNickname,
            passengerNickname = event.passengerNickname,
        )
        liveNotifier.notify(listOf(event.passengerMemberId, event.driverMemberId), LiveEvent(LiveEventType.DETOUR_CONFIRM_REQUESTED, data = data))
    }

    @TransactionalEventListener
    fun on(event: AttendanceChangedEvent) {
        val data = AttendanceData(
            crewId = event.crewId,
            crewMemberId = event.crewMemberId,
            nickname = event.nickname,
            serviceDate = event.serviceDate,
            attending = event.attending,
            boardPlace = event.boardPlace?.let { PlaceData(it.placeId, it.name, it.latitude, it.longitude) },
        )
        liveNotifier.notify(event.recipientMemberIds, LiveEvent(LiveEventType.RIDER_ATTENDANCE_CHANGED, data = data))
    }
}
