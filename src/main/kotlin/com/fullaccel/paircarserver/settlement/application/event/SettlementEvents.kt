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
