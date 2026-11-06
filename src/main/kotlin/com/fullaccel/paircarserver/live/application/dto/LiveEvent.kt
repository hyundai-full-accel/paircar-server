package com.fullaccel.paircarserver.live.application.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal
import java.time.Instant

// 클라이언트(동승자 웹, 차량 앱)로 보내는 실시간 이벤트. SSE event 이름은 type.value
@JsonInclude(JsonInclude.Include.NON_NULL)
data class LiveEvent(
    val type: LiveEventType,
    val tripId: Long,
    val data: LiveEventData? = null, // 내용이 없는 이벤트(trip.ended)는 생략
    val occurredAt: Instant = Instant.now(),
)

enum class LiveEventType(@get:JsonValue val value: String) {
    // 동승자가 받음
    TRIP_STARTED("trip.started"),
    VEHICLE_LOCATION("vehicle.location"),
    VEHICLE_APPROACHING("vehicle.approaching"),
    VEHICLE_ARRIVED("vehicle.arrived"),
    RIDER_BOARDED("rider.boarded"),
    DRIVER_DELAYED("driver.delayed"),
    TRIP_ENDED("trip.ended"),

    // 운전자(차량 앱)가 받아 TTS로 읽음
    RIDER_DELAYED("rider.delayed"),
    RIDER_HERE("rider.here"),
    RIDER_ABSENT("rider.absent"),
    RIDER_PLACE_CHANGED("rider.place-changed"),
}

// 이벤트 종류별 내용
sealed interface LiveEventData

data class DriverData(
    val driverNickname: String,
) : LiveEventData

data class DriverDelayData(
    val driverNickname: String,
    val minutes: Int,
) : LiveEventData

data class VehicleLocationData(
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val remainSeconds: Int?, // 이 동승자 경유지(또는 목적지)까지 남은 시간
    val remainMeters: Int?,
) : LiveEventData

data class RiderData(
    val crewMemberId: Long,
    val nickname: String,
) : LiveEventData

data class RiderDelayData(
    val crewMemberId: Long,
    val nickname: String,
    val minutes: Int,
) : LiveEventData

data class RiderPlaceData(
    val crewMemberId: Long,
    val nickname: String,
    val placeId: Long,
    val placeName: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
) : LiveEventData
