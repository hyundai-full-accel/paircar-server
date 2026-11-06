package com.fullaccel.paircarserver.trip.api

import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import com.fullaccel.paircarserver.trip.api.dto.BoardingEventRequest
import com.fullaccel.paircarserver.trip.api.dto.ChangeBoardPlaceRequest
import com.fullaccel.paircarserver.trip.api.dto.EndTripRequest
import com.fullaccel.paircarserver.trip.api.dto.StartTripRequest
import com.fullaccel.paircarserver.trip.api.dto.UploadSegmentsRequest
import com.fullaccel.paircarserver.trip.application.TripService
import com.fullaccel.paircarserver.trip.application.dto.TripResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "운행",
    description = "차량 앱이 호출. 쓰기 요청은 그 운행의 운전자만 할 수 있음(탈 지점 변경, 불참은 동승자 본인도 가능). 모든 응답은 운행 전체 상태(TripResult)를 돌려줌. " +
        "탑승, 하차, 구간, 종료는 같은 요청을 다시 보내도 결과가 같음",
)
@RestController
@RequestMapping("/api/trips")
class TripController(
    private val tripService: TripService,
    private val crewService: CrewService,
) {

    @Operation(
        summary = "운행 시작 (시동)",
        description = "운행과 동승자별 탑승 행을 만들고, 응답의 waypoints(출발지, 목적지를 뺀 경유지)로 경유지를 설정합니다. " +
            "검증: 크루 소속, 차량 등록, 장소 소속, 동승자(운전자 제외, 중복 없음), 운전자 계좌, 경유지 최대 2곳, 진행 중인 운행 없음.",
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun startTrip(
        @LoginMember memberId: Long,
        @Valid @RequestBody request: StartTripRequest,
    ): ApiResponse<TripResult> =
        ApiResponse.ok(tripService.startTrip(memberId, request.toCommand()))

    @Operation(summary = "운행 조회", description = "그 크루 멤버만 조회할 수 있습니다.")
    @GetMapping("/{tripId}")
    fun getTrip(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
    ): ApiResponse<TripResult> {
        val trip = tripService.getTrip(tripId)
        crewService.validateCrewAccess(trip.crewId, memberId)
        return ApiResponse.ok(trip)
    }

    @Operation(summary = "탑승 기록", description = "탑승 감지(도어 → 벨트) 시 호출합니다. 이미 기록됐으면 처음 값을 유지합니다.")
    @PostMapping("/{tripId}/boardings/{crewMemberId}/board")
    fun recordBoarding(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @PathVariable crewMemberId: Long,
        @Valid @RequestBody request: BoardingEventRequest,
    ): ApiResponse<TripResult> =
        ApiResponse.ok(tripService.recordBoarding(memberId, tripId, crewMemberId, request.toCommand()))

    @Operation(summary = "하차 기록", description = "탑승 전이면 BOARDING_NOT_BOARDED입니다. 이미 기록됐으면 처음 값을 유지합니다.")
    @PostMapping("/{tripId}/boardings/{crewMemberId}/alight")
    fun recordAlighting(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @PathVariable crewMemberId: Long,
        @Valid @RequestBody request: BoardingEventRequest,
    ): ApiResponse<TripResult> =
        ApiResponse.ok(tripService.recordAlighting(memberId, tripId, crewMemberId, request.toCommand()))

    @Operation(
        summary = "탈 지점 변경",
        description = "\"오늘은 후문에서 탈게요\". 운전자나 동승자 본인이 탑승 전에만 할 수 있습니다. " +
            "운전자는 응답의 waypoints로 경유지를 한 번 갱신하고, 동승자가 바꾸면 운전자에게 rider.place-changed가 갑니다.",
    )
    @PutMapping("/{tripId}/boardings/{crewMemberId}/board-place")
    fun changeBoardPlace(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @PathVariable crewMemberId: Long,
        @Valid @RequestBody request: ChangeBoardPlaceRequest,
    ): ApiResponse<TripResult> =
        ApiResponse.ok(tripService.changeBoardPlace(memberId, tripId, crewMemberId, request.placeId))

    @Operation(
        summary = "불참",
        description = "\"오늘은 안 타요\". 운전자나 동승자 본인이 탑승 전에만 할 수 있고, 이 운행 정산에서 빠집니다. " +
            "동승자가 하면 운전자에게 rider.absent가 갑니다.",
    )
    @DeleteMapping("/{tripId}/boardings/{crewMemberId}")
    fun cancelBoarding(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @PathVariable crewMemberId: Long,
    ): ApiResponse<TripResult> =
        ApiResponse.ok(tripService.cancelBoarding(memberId, tripId, crewMemberId))

    @Operation(
        summary = "구간 업로드",
        description = "탑승 인원이 바뀌는 지점마다 나눈 구간입니다. 이미 저장된 seq는 건너뜁니다. " +
            "이어지는 seq끼리는 앞 구간 끝과 다음 구간 시작이 같아야 하고, 동승자는 이 운행에 탑승한 사람이어야 합니다.",
    )
    @PostMapping("/{tripId}/segments")
    fun uploadSegments(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @Valid @RequestBody request: UploadSegmentsRequest,
    ): ApiResponse<TripResult> =
        ApiResponse.ok(tripService.uploadSegments(memberId, tripId, request.toCommands()))

    @Operation(summary = "운행 종료", description = "하차 기록이 없는 동승자는 도착 시각, 도착 주행거리로 하차 처리합니다. 이미 끝난 운행이면 그대로 반환합니다.")
    @PostMapping("/{tripId}/end")
    fun endTrip(
        @LoginMember memberId: Long,
        @PathVariable tripId: Long,
        @Valid @RequestBody request: EndTripRequest,
    ): ApiResponse<TripResult> =
        ApiResponse.ok(tripService.endTrip(memberId, tripId, request.toCommand()))
}
