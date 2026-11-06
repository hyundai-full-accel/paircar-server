package com.fullaccel.paircarserver.settlement.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ReceiptRepository : JpaRepository<Receipt, Long> {

    fun existsByPayerIdAndPayeeIdAndMonth(payerId: Long, payeeId: Long, month: LocalDate): Boolean

    @EntityGraph(attributePaths = ["payer.member", "payee.member"])
    fun findAllByPayerCrewIdAndMonthOrderById(crewId: Long, month: LocalDate): List<Receipt>
}
