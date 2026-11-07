package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.DetourData
import com.fullaccel.paircarserver.live.application.dto.DriverData
import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import com.fullaccel.paircarserver.live.application.dto.LiveEventType
import com.fullaccel.paircarserver.live.application.dto.PushUrgency
import com.fullaccel.paircarserver.live.application.dto.ReceiptData
import com.fullaccel.paircarserver.live.application.dto.ReceiptInquiryData
import com.fullaccel.paircarserver.live.application.dto.VehicleLocationData
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushMessagesTest {

    @Test
    fun `출발 알림은 운전자 이름과 운행 화면 주소를 담는다`() {
        val message = PushMessages.of(LiveEvent(LiveEventType.TRIP_STARTED, 7, DriverData("지훈")))!!

        assertEquals("지훈님이 출발했어요", message.title)
        assertEquals("/trips/7", message.url)
    }

    @Test
    fun `곧 도착은 바로 봐야 하는 알림이다`() {
        val message = PushMessages.of(LiveEvent(LiveEventType.VEHICLE_APPROACHING, 7))!!

        assertEquals(PushUrgency.HIGH, message.urgency)
        assertEquals(300, message.ttlSeconds)
    }

    @Test
    fun `영수증 알림은 월과 금액을 보여준다`() {
        val message = PushMessages.of(LiveEvent(LiveEventType.RECEIPT_ISSUED, data = ReceiptData(3, "2026-11", 20_700, "지훈")))!!

        assertEquals("11월 영수증이 도착했어요", message.title)
        assertEquals("보낼 금액 20,700원", message.body)
        assertEquals("/receipts/3", message.url)
    }

    @Test
    fun `기록 확인 요청은 운전자에게 누가 몇 월 영수증을 물었는지 보여준다`() {
        val data = ReceiptInquiryData(receiptId = 3, inquiryId = 9, month = "2026-11", requesterNickname = "수아", message = "11월 2일 거리가 이상해요")
        val message = PushMessages.of(LiveEvent(LiveEventType.RECEIPT_INQUIRY_CREATED, data = data))!!

        assertEquals("수아님이 11월 영수증 확인을 요청했어요", message.title)
        assertEquals("11월 2일 거리가 이상해요", message.body)
        assertEquals("/receipts/3", message.url)
    }

    @Test
    fun `우회 거리 확인 요청은 장소와 거리를 보여준다`() {
        val data = DetourData(detourId = 1, crewId = 2, placeName = "○○아파트 정문", detourKm = BigDecimal("3.0"), driverNickname = "지훈", passengerNickname = "수아")

        val message = PushMessages.of(LiveEvent(LiveEventType.DETOUR_CONFIRM_REQUESTED, data = data))!!

        assertEquals("○○아파트 정문 3.0km", message.body)
    }

    @Test
    fun `차량 위치처럼 계속 바뀌는 이벤트는 폰 알림으로 보내지 않는다`() {
        val data = VehicleLocationData(BigDecimal("37.4"), BigDecimal("127.1"), 180, 1200)

        assertNull(PushMessages.of(LiveEvent(LiveEventType.VEHICLE_LOCATION, 7, data)))
        assertNull(PushMessages.of(LiveEvent(LiveEventType.TRIP_ENDED, 7)))
    }
}
