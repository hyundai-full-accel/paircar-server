package com.fullaccel.paircarserver.settlement.application.dto

import com.fullaccel.paircarserver.settlement.domain.ReceiptInquiry
import java.time.Instant

data class ReceiptInquiryResult(
    val inquiryId: Long,
    val receiptId: Long,
    val requesterCrewMemberId: Long,
    val requesterNickname: String,
    val message: String,
    val createdAt: Instant,
    val resolvedAt: Instant?,
) {
    companion object {
        fun from(inquiry: ReceiptInquiry) = ReceiptInquiryResult(
            inquiryId = inquiry.id,
            receiptId = inquiry.receipt.id,
            requesterCrewMemberId = inquiry.requester.id,
            requesterNickname = inquiry.requester.member.nickname,
            message = inquiry.message,
            createdAt = inquiry.createdAt,
            resolvedAt = inquiry.resolvedAt,
        )
    }
}
