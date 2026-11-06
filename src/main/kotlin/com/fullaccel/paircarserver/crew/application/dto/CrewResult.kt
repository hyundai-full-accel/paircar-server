package com.fullaccel.paircarserver.crew.application.dto

import com.fullaccel.paircarserver.crew.domain.Crew
import com.fullaccel.paircarserver.crew.domain.SettlementMode

data class CrewResult(
    val crewId: Long,
    val name: String,
    val settlementMode: SettlementMode,
    val includeDriver: Boolean,
    val monthlyFixedWon: Int?,
    val manualFuelPriceWon: Int?,
) {
    companion object {
        fun from(crew: Crew) = CrewResult(
            crewId = crew.id,
            name = crew.name,
            settlementMode = crew.settlementMode,
            includeDriver = crew.includeDriver,
            monthlyFixedWon = crew.monthlyFixedWon,
            manualFuelPriceWon = crew.manualFuelPriceWon,
        )
    }
}
