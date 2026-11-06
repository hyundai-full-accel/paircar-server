package com.fullaccel.paircarserver.member.api.dto

import com.fullaccel.paircarserver.member.application.dto.BankAccountCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateMemberRequest(
    @field:Schema(description = "크루 안에서 보이는 이름 (TTS로 읽음)", example = "지훈")
    @field:NotBlank
    @field:Size(max = 30)
    val nickname: String,
)

data class BankAccountRequest(
    @field:Schema(example = "○○은행")
    @field:NotBlank
    @field:Size(max = 30)
    val bankName: String,

    @field:Schema(example = "3333-01-1234567")
    @field:NotBlank
    @field:Size(max = 30)
    val accountNumber: String,

    @field:Schema(example = "지훈")
    @field:NotBlank
    @field:Size(max = 30)
    val accountHolder: String,
) {
    fun toCommand() = BankAccountCommand(bankName, accountNumber, accountHolder)
}
