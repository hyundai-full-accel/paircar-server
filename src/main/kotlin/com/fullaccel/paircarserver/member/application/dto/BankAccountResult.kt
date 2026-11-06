package com.fullaccel.paircarserver.member.application.dto

import com.fullaccel.paircarserver.member.domain.MemberBankAccount

data class BankAccountResult(
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
) {
    companion object {
        fun from(account: MemberBankAccount) = BankAccountResult(
            bankName = account.bankName,
            accountNumber = account.accountNumber,
            accountHolder = account.accountHolder,
        )
    }
}
