package com.fullaccel.paircarserver.settlement.api

import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import com.fullaccel.paircarserver.settlement.api.dto.ReceiptInquiryRequest
import com.fullaccel.paircarserver.settlement.application.ReceiptInquiryService
import com.fullaccel.paircarserver.settlement.application.dto.ReceiptInquiryResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "정산")
@RestController
@RequestMapping("/api/receipts/{receiptId}/inquiries")
class ReceiptInquiryController(
    private val receiptInquiryService: ReceiptInquiryService,
) {

    @Operation(
        summary = "기록 확인 요청",
        description = "영수증의 내는 사람이 \"이 기록에 문제가 있나요?\"로 운전자에게 확인을 요청합니다. " +
            "영수증은 바뀌지 않습니다. 운전자에게 receipt.inquiry-created(SSE + Push)가 갑니다.",
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createInquiry(
        @LoginMember memberId: Long,
        @PathVariable receiptId: Long,
        @Valid @RequestBody request: ReceiptInquiryRequest,
    ): ApiResponse<ReceiptInquiryResult> =
        ApiResponse.ok(receiptInquiryService.createInquiry(memberId, receiptId, request.message.trim()))

    @Operation(summary = "기록 확인 요청 목록", description = "영수증의 내는 사람, 받는 사람만 볼 수 있습니다.")
    @GetMapping
    fun getInquiries(
        @LoginMember memberId: Long,
        @PathVariable receiptId: Long,
    ): ApiResponse<List<ReceiptInquiryResult>> =
        ApiResponse.ok(receiptInquiryService.getInquiries(memberId, receiptId))

    @Operation(summary = "기록 확인 처리", description = "받는 사람(운전자)이 확인하고 처리했다고 표시합니다.")
    @PostMapping("/{inquiryId}/resolve")
    fun resolveInquiry(
        @LoginMember memberId: Long,
        @PathVariable receiptId: Long,
        @PathVariable inquiryId: Long,
    ): ApiResponse<ReceiptInquiryResult> =
        ApiResponse.ok(receiptInquiryService.resolveInquiry(memberId, receiptId, inquiryId))
}
