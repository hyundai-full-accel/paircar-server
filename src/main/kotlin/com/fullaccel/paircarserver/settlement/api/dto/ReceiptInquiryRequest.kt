package com.fullaccel.paircarserver.settlement.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ReceiptInquiryRequest(
    @field:Schema(description = "무엇이 이상한지", example = "11월 12일은 안 탔는데 기록에 있어요")
    @field:NotBlank
    @field:Size(max = 500)
    val message: String,
)
