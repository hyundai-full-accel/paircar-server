package com.fullaccel.paircarserver.settlement.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ReceiptRepository : JpaRepository<Receipt, Long>
