package com.fullaccel.paircarserver.crew.application.dto

import com.fullaccel.paircarserver.crew.domain.CrewPlace
import java.math.BigDecimal

data class CrewPlaceResult(
    val placeId: Long,
    val crewId: Long,
    val name: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
) {
    companion object {
        fun from(place: CrewPlace) = CrewPlaceResult(
            placeId = place.id,
            crewId = place.crew.id,
            name = place.name,
            latitude = place.latitude,
            longitude = place.longitude,
        )
    }
}
