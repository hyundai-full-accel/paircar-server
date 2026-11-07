package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import com.fullaccel.paircarserver.live.application.dto.LiveEventType
import com.fullaccel.paircarserver.live.application.dto.ReceiptData
import com.fullaccel.paircarserver.live.application.dto.ReceiptInquiryData
import com.fullaccel.paircarserver.settlement.application.event.ReceiptInquiryCreatedEvent
import com.fullaccel.paircarserver.settlement.application.event.ReceiptIssuedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

// 정산 모듈의 도메인 이벤트를 실시간 알림으로 보냄 (커밋 뒤)
@Component
class SettlementLiveEventListener(
    private val liveNotifier: LiveNotifier,
) {

    // 내는 사람(동승자)에게 "영수증이 도착했어요"
    @TransactionalEventListener
    fun on(event: ReceiptIssuedEvent) {
        val data = ReceiptData(
            receiptId = event.receiptId,
            month = event.month.toString(),
            amountWon = event.amountWon,
            payeeNickname = event.payeeNickname,
        )
        liveNotifier.notify(listOf(event.payerMemberId), LiveEvent(LiveEventType.RECEIPT_ISSUED, data = data))
    }

    // 받는 사람(운전자)에게 "영수증 확인을 요청했어요"
    @TransactionalEventListener
    fun on(event: ReceiptInquiryCreatedEvent) {
        val data = ReceiptInquiryData(
            receiptId = event.receiptId,
            inquiryId = event.inquiryId,
            month = event.month.toString(),
            requesterNickname = event.requesterNickname,
            message = event.message,
        )
        liveNotifier.notify(listOf(event.payeeMemberId), LiveEvent(LiveEventType.RECEIPT_INQUIRY_CREATED, data = data))
    }
}
