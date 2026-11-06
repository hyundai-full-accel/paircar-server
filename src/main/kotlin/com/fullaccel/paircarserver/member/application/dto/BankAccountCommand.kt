package com.fullaccel.paircarserver.member.application.dto

data class BankAccountCommand(
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
)
