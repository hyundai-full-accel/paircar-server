package com.fullaccel.paircarserver.live.application.dto

import java.math.BigDecimal

// 차량 앱이 운행 중 주기적으로 보내는 위치와 남은 시간 (저장하지 않고 전달만 함)
data class VehicleLocationCommand(
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val remainsByRider: List<RiderRemain> = emptyList(),
)

// 동승자별 남은 시간, 거리 (NaviHelper WaypointInfo). 탄 동승자는 목적지까지, 안 탄 동승자는 경유지까지
data class RiderRemain(
    val crewMemberId: Long,
    val remainSeconds: Int?,
    val remainMeters: Int?,
)
