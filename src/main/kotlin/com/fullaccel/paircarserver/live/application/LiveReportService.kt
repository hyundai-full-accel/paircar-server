package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.live.application.dto.VehicleLocationCommand

// 운행 중 저장하지 않고 상대에게 전달만 하는 알림. 끝난 운행이면 TRIP_ALREADY_ENDED
interface LiveReportService {

    // 운전자 → 아직 내리지 않은 동승자들: 차량 위치와 동승자별 남은 시간
    fun reportVehicleLocation(driverMemberId: Long, tripId: Long, command: VehicleLocationCommand)

    // 운전자 → 그 동승자: "곧 도착" (승차 지점 300m 접근)
    fun reportApproaching(driverMemberId: Long, tripId: Long, riderCrewMemberId: Long)

    // 운전자 → 그 동승자: "차가 도착했어요" (경유지 도착, 아직 미탑승)
    fun reportArrived(driverMemberId: Long, tripId: Long, riderCrewMemberId: Long)

    // 운전자 → 동승자들: "지훈님이 10분 늦어요" (Gleo NOTIFY_CREW_DELAY)
    fun notifyDriverDelay(driverMemberId: Long, tripId: Long, minutes: Int)

    // 동승자 본인 → 운전자: "5분 늦어요" (경로는 바꾸지 않음)
    fun notifyRiderDelay(riderMemberId: Long, tripId: Long, minutes: Int)

    // 동승자 본인 → 운전자: "나와 있어요"
    fun notifyRiderHere(riderMemberId: Long, tripId: Long)
}
