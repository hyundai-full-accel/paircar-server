package com.fullaccel.paircarserver.trip.application

import com.fullaccel.paircarserver.trip.application.dto.BoardingEventCommand
import com.fullaccel.paircarserver.trip.application.dto.EndTripCommand
import com.fullaccel.paircarserver.trip.application.dto.SegmentCommand
import com.fullaccel.paircarserver.trip.application.dto.SettlementTrip
import com.fullaccel.paircarserver.trip.application.dto.StartTripCommand
import com.fullaccel.paircarserver.trip.application.dto.TripResult
import com.fullaccel.paircarserver.trip.application.dto.TripSummaryResult
import com.fullaccel.paircarserver.trip.domain.Boarding
import java.time.LocalDate
import java.time.YearMonth

// 운행, 탑승, 구간. 쓰기 요청은 그 운행의 운전자(driverMemberId)만 할 수 있음
// 예외: 탈 지점 변경, 불참은 동승자 본인도 할 수 있음 (requesterMemberId)
// 탑승, 하차, 구간, 종료는 같은 요청이 다시 와도 결과가 같음 (차량 앱 오프라인 버퍼 재전송 대비)
// 상태가 바뀌면 커밋 뒤에 trip.application.event의 이벤트를 발행함
interface TripService {

    // 검증: 크루 소속, 차량 등록(규칙 1), 장소 소속(규칙 2, 4), 동승자 소속과 운전자 제외(규칙 3),
    // 운전자 계좌(규칙 5), 경유지 최대 2곳(규칙 7), 진행 중인 운행 없음
    // 진행 중인 운행이 있으면 TRIP_ALREADY_IN_PROGRESS와 함께 그 운행 ID(TripInProgress)를 실어 보냄
    // 결과의 waypoints로 차량 앱이 경유지를 설정함
    fun startTrip(driverMemberId: Long, command: StartTripCommand): TripResult

    fun getTrip(tripId: Long): TripResult

    // 크루의 진행 중인 운행. 없으면 null
    fun getCurrentTrip(crewId: Long): TripResult?

    // 크루의 그 달(운행일 KST) 운행 목록. 시작 순
    fun getTripsOfMonth(crewId: Long, month: YearMonth): List<TripSummaryResult>

    // 동승자의 "탔어요". 아직 탑승 기록이 없으면 지금 시각으로 탑승 처리하고 운전자에게 알림 (주행거리는 비워 둠)
    // 이미 탔으면 그대로. 그 운행의 동승자가 아니면 TRIP_ACCESS_DENIED, 끝난 운행이면 TRIP_ALREADY_ENDED
    fun reportBoardedByRider(riderMemberId: Long, tripId: Long): TripResult

    // 이미 탑승 기록이 있으면 처음 값을 유지함. 단 동승자가 먼저 알린 탑승이면 주행거리만 채움
    fun recordBoarding(driverMemberId: Long, tripId: Long, crewMemberId: Long, command: BoardingEventCommand): TripResult

    // 탑승 전이면 BOARDING_NOT_BOARDED. 이미 하차 기록이 있으면 처음 값을 유지함
    fun recordAlighting(driverMemberId: Long, tripId: Long, crewMemberId: Long, command: BoardingEventCommand): TripResult

    // "오늘은 후문에서 탈게요". 탑승 전에만 가능. 운전자나 동승자 본인이 요청
    fun changeBoardPlace(requesterMemberId: Long, tripId: Long, crewMemberId: Long, placeId: Long): TripResult

    // "오늘은 안 타요". 탑승 전에만 가능. 탑승 행을 지워 정산에서 빠짐. 운전자나 동승자 본인이 요청
    fun cancelBoarding(requesterMemberId: Long, tripId: Long, crewMemberId: Long): TripResult

    // 이미 저장된 seq는 건너뜀. 구간은 seq 순서대로 이어져야 하고(규칙 8), 동승자는 이 운행에 탄 사람이어야 함
    fun uploadSegments(driverMemberId: Long, tripId: Long, segments: List<SegmentCommand>): TripResult

    // 탑승했는데 하차 기록이 없는 동승자는 도착 시각, 도착 주행거리로 하차 처리함
    // 이미 끝난 운행이면 그대로 반환함
    fun endTrip(driverMemberId: Long, tripId: Long, command: EndTripCommand): TripResult

    // 정산용 조회: 크루의 끝난 운행 중 운행일이 until 이전이고 구간이 올라온 운행. 탑승한 동승자만 포함
    fun getSettlementTrips(crewId: Long, until: LocalDate): List<SettlementTrip>

    // 다른 패키지 서비스가 연관관계를 맺을 때 쓰는 엔티티 조회. 트랜잭션 안에서만 사용
    fun getBoardingEntity(boardingId: Long): Boarding
}
