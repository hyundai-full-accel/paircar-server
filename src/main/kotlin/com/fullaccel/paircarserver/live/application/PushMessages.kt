package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.DetourData
import com.fullaccel.paircarserver.live.application.dto.DriverData
import com.fullaccel.paircarserver.live.application.dto.DriverDelayData
import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import com.fullaccel.paircarserver.live.application.dto.LiveEventType
import com.fullaccel.paircarserver.live.application.dto.PushMessage
import com.fullaccel.paircarserver.live.application.dto.PushUrgency
import com.fullaccel.paircarserver.live.application.dto.ReceiptData
import com.fullaccel.paircarserver.live.application.dto.ReceiptInquiryData
import java.text.NumberFormat
import java.util.Locale

// 실시간 이벤트 중 폰 알림으로도 보낼 것과 그 문구. null이면 SSE로만 보냄
// url은 동승자 웹 안의 경로 (서비스 워커가 자기 주소 기준으로 엶)
object PushMessages {

    private const val FIVE_MINUTES = 5 * 60
    private const val ONE_HOUR = 60 * 60
    private const val ONE_DAY = 24 * 60 * 60

    fun of(event: LiveEvent): PushMessage? {
        val type = event.type.value
        return when (event.type) {
            LiveEventType.TRIP_STARTED -> {
                val data = event.data as DriverData
                PushMessage(type, "${data.driverNickname}님이 출발했어요", "도착 예정 시간을 확인해 보세요", tripUrl(event), ONE_HOUR, PushUrgency.NORMAL)
            }
            LiveEventType.VEHICLE_APPROACHING ->
                PushMessage(type, "차가 곧 도착해요", "지금 나가면 딱 맞아요", tripUrl(event), FIVE_MINUTES, PushUrgency.HIGH)
            LiveEventType.VEHICLE_ARRIVED ->
                PushMessage(type, "차가 도착했어요", "승차 지점에서 기다리고 있어요", tripUrl(event), FIVE_MINUTES, PushUrgency.HIGH)
            LiveEventType.DRIVER_DELAYED -> {
                val data = event.data as DriverDelayData
                PushMessage(type, "${data.driverNickname}님이 ${data.minutes}분 늦어요", "천천히 나오셔도 돼요", tripUrl(event), ONE_HOUR, PushUrgency.NORMAL)
            }
            LiveEventType.DETOUR_CONFIRM_REQUESTED -> {
                val data = event.data as DetourData
                PushMessage(type, "우회 거리를 확인해 주세요", "${data.placeName} ${data.detourKm.toPlainString()}km", "/crews/${data.crewId}/detours", ONE_DAY, PushUrgency.NORMAL)
            }
            LiveEventType.RECEIPT_ISSUED -> {
                val data = event.data as ReceiptData
                val monthNumber = data.month.substringAfter('-').toInt()
                PushMessage(type, "${monthNumber}월 영수증이 도착했어요", "보낼 금액 ${won(data.amountWon)}", "/receipts/${data.receiptId}", ONE_DAY, PushUrgency.NORMAL)
            }
            LiveEventType.RECEIPT_INQUIRY_CREATED -> {
                val data = event.data as ReceiptInquiryData
                val monthNumber = data.month.substringAfter('-').toInt()
                PushMessage(type, "${data.requesterNickname}님이 ${monthNumber}월 영수증 확인을 요청했어요", data.message.take(60), "/receipts/${data.receiptId}", ONE_DAY, PushUrgency.NORMAL)
            }
            else -> null
        }
    }

    private fun tripUrl(event: LiveEvent) = "/trips/${event.tripId}"

    private fun won(amount: Int) = NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
}
