package com.fullaccel.paircarserver.settlement.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ReceiptItemRepository : JpaRepository<ReceiptItem, Long>
