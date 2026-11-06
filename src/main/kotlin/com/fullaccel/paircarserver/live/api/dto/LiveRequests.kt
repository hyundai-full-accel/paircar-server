package com.fullaccel.paircarserver.live.api.dto

import com.fullaccel.paircarserver.live.application.dto.RiderRemain
import com.fullaccel.paircarserver.live.application.dto.VehicleLocationCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

data class VehicleLocationRequest(
    @field:Schema(example = "37.401234")
    @field:DecimalMin("-90")
    @field:DecimalMax("90")
    val latitude: BigDecimal,

    @field:Schema(example = "127.108765")
    @field:DecimalMin("-180")
    @field:DecimalMax("180")
    val longitude: BigDecimal,

    @field:Schema(description = "동승자별 남은 시간, 거리 (NaviHelper WaypointInfo)")
    @field:Valid
    val remainsByRider: List<RiderRemainRequest> = emptyList(),
) {
    fun toCommand() = VehicleLocationCommand(
        latitude = latitude,
        longitude = longitude,
        remainsByRider = remainsByRider.map { RiderRemain(it.crewMemberId, it.remainSeconds, it.remainMeters) },
    )
}

data class RiderRemainRequest(
    val crewMemberId: Long,
    @field:Schema(description = "남은 시간 (초)", example = "180")
    @field:PositiveOrZero
    val remainSeconds: Int? = null,
    @field:Schema(description = "남은 거리 (m)", example = "1200")
    @field:PositiveOrZero
    val remainMeters: Int? = null,
)

data class DelayRequest(
    @field:Schema(description = "늦는 시간 (분)", example = "5")
    @field:Min(1)
    @field:Max(120)
    val minutes: Int,
)
