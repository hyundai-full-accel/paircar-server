package com.fullaccel.paircarserver.global.error

import org.springframework.http.HttpStatus

// 응답의 error.code로 나가는 값. 도메인 에러는 기능을 만들 때 아래에 추가함
// 재시도 기준: 5xx만 재시도. 4xx는 같은 요청을 다시 보내도 실패하므로 버림
enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "이미 존재하거나 다른 데이터와 충돌합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 토큰이 없거나 유효하지 않습니다."),

    // 멤버
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),

    // 차량
    CAR_NOT_FOUND(HttpStatus.NOT_FOUND, "차량을 찾을 수 없습니다."),
    CAR_NOT_REGISTERED_TO_MEMBER(HttpStatus.FORBIDDEN, "멤버가 등록하지 않은 차량입니다."),

    // 크루
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "크루를 찾을 수 없습니다."),
    CREW_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "크루 멤버를 찾을 수 없습니다."),
    CREW_ACCESS_DENIED(HttpStatus.FORBIDDEN, "크루 멤버만 요청할 수 있습니다."),
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "초대 코드를 찾을 수 없습니다."),
    INVITE_EXPIRED(HttpStatus.GONE, "만료된 초대 코드입니다."),
    CREW_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 크루에 참여한 멤버입니다."),
    CREW_MISMATCH(HttpStatus.BAD_REQUEST, "같은 크루 소속이 아닙니다."),
    CREW_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "크루 장소를 찾을 수 없습니다."),
    CREW_PLACE_NAME_DUPLICATED(HttpStatus.CONFLICT, "크루에 같은 이름의 장소가 이미 있습니다."),
    DETOUR_NOT_FOUND(HttpStatus.NOT_FOUND, "우회 거리를 찾을 수 없습니다."),
    DETOUR_SAME_PASSENGER_AND_DRIVER(HttpStatus.BAD_REQUEST, "동승자와 운전자가 같을 수 없습니다."),
    DETOUR_CONFIRMER_NOT_PARTY(HttpStatus.FORBIDDEN, "우회 거리의 운전자나 동승자만 확인할 수 있습니다."),

    // 운행
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "운행을 찾을 수 없습니다."),
    TRIP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 운행의 운전자만 요청할 수 있습니다."),
    TRIP_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "이미 진행 중인 운행이 있습니다."),
    TRIP_ALREADY_ENDED(HttpStatus.CONFLICT, "이미 끝난 운행입니다."),
    BANK_ACCOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "운행을 시작하려면 운전자 계좌를 먼저 등록해야 합니다."),
    TOO_MANY_WAYPOINTS(HttpStatus.BAD_REQUEST, "경유지는 최대 2곳입니다."),
    INVALID_ODOMETER(HttpStatus.BAD_REQUEST, "누적 주행거리가 이전 값보다 작습니다."),
    BOARDING_NOT_FOUND(HttpStatus.NOT_FOUND, "탑승 정보를 찾을 수 없습니다."),
    BOARDING_DRIVER_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "운전자는 동승자로 탑승할 수 없습니다."),
    BOARDING_DUPLICATED(HttpStatus.BAD_REQUEST, "같은 동승자가 중복되었습니다."),
    BOARDING_ALREADY_BOARDED(HttpStatus.CONFLICT, "이미 탑승한 동승자입니다."),
    BOARDING_NOT_BOARDED(HttpStatus.CONFLICT, "아직 탑승하지 않은 동승자입니다."),
    INVALID_SEGMENT(HttpStatus.BAD_REQUEST, "구간 정보가 올바르지 않습니다."),

    // 정산
    RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "영수증을 찾을 수 없습니다."),
    RECEIPT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "영수증의 동승자나 운전자만 조회할 수 있습니다."),
    FUEL_PRICE_NOT_FOUND(HttpStatus.CONFLICT, "정산 월의 유가 정보가 없습니다. 크루 유가를 직접 입력해 주세요."),
    SETTLEMENT_MODE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "아직 지원하지 않는 정산 방식입니다."),
}
