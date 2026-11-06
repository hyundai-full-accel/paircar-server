package com.fullaccel.paircarserver.settlement.application

import com.fullaccel.paircarserver.settlement.application.dto.ReceiptResult
import java.time.YearMonth

// 월 정산과 영수증 (기술명세 T5)
interface SettlementService {

    // 크루의 월 영수증을 (동승자, 운전자)마다 발행하고 새로 발행한 영수증을 반환함
    // 대상: 운행일이 정산 월 말일 이전이고 아직 정산되지 않은 탑승 (지난달에 늦게 올라온 탑승 포함)
    // 이미 발행된 (동승자, 운전자, 월) 영수증은 바꾸지 않음. 그 쌍의 늦은 탑승은 다음 달로 넘어감
    // 우회 거리는 운전자, 동승자가 둘 다 확인한 값만 씀
    // 영수증마다 ReceiptIssuedEvent를 발행함
    fun issueReceipts(crewId: Long, month: YearMonth): List<ReceiptResult>

    fun getReceipt(receiptId: Long): ReceiptResult

    fun getReceipts(crewId: Long, month: YearMonth): List<ReceiptResult>
}
