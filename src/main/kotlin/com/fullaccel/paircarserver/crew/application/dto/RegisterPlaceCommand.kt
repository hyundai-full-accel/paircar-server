package com.fullaccel.paircarserver.crew.application.dto

import java.math.BigDecimal

data class RegisterPlaceCommand(
    val name: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
)
