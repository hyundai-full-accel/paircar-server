package com.fullaccel.paircarserver.settlement.api

import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.global.response.ApiResponse
import com.fullaccel.paircarserver.settlement.application.SettlementService
import com.fullaccel.paircarserver.settlement.application.dto.ReceiptResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@Tag(name = "정산", description = "월 영수증 발행과 조회 (기술명세 T5). 영수증은 그 영수증의 동승자, 운전자만 볼 수 있음")
@RestController
class SettlementController(
    private val settlementService: SettlementService,
    private val crewService: CrewService,
) {

    @Operation(
        summary = "월 영수증 발행",
        description = "크루의 정산 월 영수증을 (동승자, 운전자)마다 발행하고 새로 발행한 영수증을 반환합니다. " +
            "이미 발행된 영수증은 바뀌지 않고, 발행 뒤에 늦게 올라온 탑승은 다음 달 영수증에 들어갑니다. " +
            "운전자, 동승자가 둘 다 확인한 우회 거리만 반영합니다.",
    )
    @PostMapping("/api/crews/{crewId}/receipts")
    fun issueReceipts(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
        @Parameter(description = "정산 월", example = "2026-11") @RequestParam month: YearMonth,
    ): ApiResponse<List<ReceiptResult>> {
        crewService.validateCrewAccess(crewId, memberId)
        return ApiResponse.ok(settlementService.issueReceipts(crewId, month))
    }

    @Operation(summary = "내 월 영수증 목록", description = "크루에서 내가 내거나 받는 그 달 영수증입니다.")
    @GetMapping("/api/crews/{crewId}/receipts")
    fun getMyReceipts(
        @LoginMember memberId: Long,
        @PathVariable crewId: Long,
        @Parameter(description = "정산 월", example = "2026-11") @RequestParam month: YearMonth,
    ): ApiResponse<List<ReceiptResult>> {
        val crewMemberId = crewService.validateCrewAccess(crewId, memberId)
        val receipts = settlementService.getReceipts(crewId, month)
            .filter { it.payerCrewMemberId == crewMemberId || it.payeeCrewMemberId == crewMemberId }
        return ApiResponse.ok(receipts)
    }

    @Operation(summary = "영수증 상세", description = "동승자 웹의 영수증 화면입니다. 하루치 내역(items)과 계좌가 들어 있습니다.")
    @GetMapping("/api/receipts/{receiptId}")
    fun getReceipt(
        @LoginMember memberId: Long,
        @PathVariable receiptId: Long,
    ): ApiResponse<ReceiptResult> {
        val receipt = settlementService.getReceipt(receiptId)
        if (memberId != receipt.payerMemberId && memberId != receipt.payeeMemberId) {
            throw BusinessException(ErrorCode.RECEIPT_ACCESS_DENIED)
        }
        return ApiResponse.ok(receipt)
    }
}
