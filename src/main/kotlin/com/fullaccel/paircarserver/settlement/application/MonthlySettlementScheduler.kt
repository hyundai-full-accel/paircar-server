package com.fullaccel.paircarserver.settlement.application

import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.global.logging.LogContext
import com.fullaccel.paircarserver.global.time.KST_ZONE_ID
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.YearMonth

// 매달 1일 새벽(KST)에 모든 크루의 지난달 영수증을 발행함. 누가 언제 발행하느냐를 사람에게 맡기지 않으려는 것
// 크루마다 따로 발행해서 한 크루가 실패해도(계좌 없음, 유가 없음) 다른 크루는 발행됨. 실패한 크루는 수동 발행으로 다시 할 수 있음
@Component
class MonthlySettlementScheduler(
    private val settlementService: SettlementService,
    private val crewService: CrewService,
    private val clock: Clock,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${paircar.settlement.auto-issue-cron:0 0 4 1 * *}", zone = KST_ZONE_ID)
    fun issueLastMonth() = LogContext.job("monthly-settlement") {
        val month = YearMonth.now(clock).minusMonths(1)
        crewService.getAllCrewIds().forEach { crewId ->
            try {
                val receipts = settlementService.issueReceipts(crewId, month)
                if (receipts.isNotEmpty()) log.info("Issued {} receipts for crew {} ({})", receipts.size, crewId, month)
            } catch (e: BusinessException) {
                if (e.errorCode != ErrorCode.SETTLEMENT_MODE_NOT_SUPPORTED) {
                    log.warn("Monthly settlement failed for crew {} ({}): {}", crewId, month, e.errorCode)
                }
            }
        }
    }
}
