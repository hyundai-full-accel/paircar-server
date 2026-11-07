package com.fullaccel.paircarserver.settlement.application

import com.fullaccel.paircarserver.settlement.application.dto.ReceiptInquiryResult

// 영수증 기록 확인 요청. 영수증은 발행 뒤 바뀌지 않으므로, 이상한 기록은 운전자에게 확인을 요청하고 둘이 이야기해서 처리함
interface ReceiptInquiryService {

    // 영수증의 내는 사람만 (RECEIPT_ACCESS_DENIED). 받는 사람(운전자)에게 ReceiptInquiryCreatedEvent
    fun createInquiry(requesterMemberId: Long, receiptId: Long, message: String): ReceiptInquiryResult

    // 영수증의 내는 사람, 받는 사람만
    fun getInquiries(requesterMemberId: Long, receiptId: Long): List<ReceiptInquiryResult>

    // 받는 사람(운전자)만 처리했다고 표시. 없으면 RECEIPT_INQUIRY_NOT_FOUND
    fun resolveInquiry(requesterMemberId: Long, receiptId: Long, inquiryId: Long): ReceiptInquiryResult
}
