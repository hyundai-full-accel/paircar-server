package com.fullaccel.paircarserver.settlement.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface ReceiptItemRepository : JpaRepository<ReceiptItem, Long> {

    fun findAllByBoardingIdIn(boardingIds: Collection<Long>): List<ReceiptItem>

    @EntityGraph(attributePaths = ["boarding.trip"])
    fun findAllByReceiptIdInOrderById(receiptIds: Collection<Long>): List<ReceiptItem>
}
