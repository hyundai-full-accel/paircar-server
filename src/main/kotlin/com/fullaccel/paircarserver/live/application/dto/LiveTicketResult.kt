package com.fullaccel.paircarserver.live.application.dto

import java.time.Instant

data class LiveTicketResult(
    val ticket: String,
    val expiresAt: Instant,
)
