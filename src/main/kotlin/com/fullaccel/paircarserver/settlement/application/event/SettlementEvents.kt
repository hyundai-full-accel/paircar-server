package com.fullaccel.paircarserver.settlement.application.event

import java.time.YearMonth

// 정산 모듈이 상태를 바꾼 뒤 발행하는 도메인 이벤트. 트랜잭션 커밋 뒤에 다른 모듈이 받음

data class ReceiptIssuedEvent(
    val receiptId: Long,
    val month: YearMonth,
    val payerMemberId: Long,
    val payeeNickname: String,
    val amountWon: Int,
)

// 내는 사람이 영수증 기록 확인을 요청함. 받는 사람(운전자)에게 알림
data class ReceiptInquiryCreatedEvent(
    val receiptId: Long,
    val inquiryId: Long,
    val month: YearMonth,
    val payeeMemberId: Long,
    val requesterNickname: String,
    val message: String,
)
