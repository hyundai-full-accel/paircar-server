package com.fullaccel.paircarserver.settlement.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface ReceiptInquiryRepository : JpaRepository<ReceiptInquiry, Long> {

    @EntityGraph(attributePaths = ["requester.member"])
    fun findAllByReceiptIdOrderById(receiptId: Long): List<ReceiptInquiry>
}
