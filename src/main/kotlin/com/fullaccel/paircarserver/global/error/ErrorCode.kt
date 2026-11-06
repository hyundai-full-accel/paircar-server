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

    // 멤버
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),

    // 차량
    CAR_NOT_FOUND(HttpStatus.NOT_FOUND, "차량을 찾을 수 없습니다."),
    CAR_NOT_REGISTERED_TO_MEMBER(HttpStatus.FORBIDDEN, "멤버가 등록하지 않은 차량입니다."),

    // 크루
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "크루를 찾을 수 없습니다."),
    CREW_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "크루 멤버를 찾을 수 없습니다."),
    CREW_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 크루에 참여한 멤버입니다."),
    CREW_MISMATCH(HttpStatus.BAD_REQUEST, "같은 크루 소속이 아닙니다."),
    CREW_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "크루 장소를 찾을 수 없습니다."),
    CREW_PLACE_NAME_DUPLICATED(HttpStatus.CONFLICT, "크루에 같은 이름의 장소가 이미 있습니다."),
    DETOUR_NOT_FOUND(HttpStatus.NOT_FOUND, "우회 거리를 찾을 수 없습니다."),
    DETOUR_SAME_PASSENGER_AND_DRIVER(HttpStatus.BAD_REQUEST, "동승자와 운전자가 같을 수 없습니다."),
    DETOUR_CONFIRMER_NOT_PARTY(HttpStatus.FORBIDDEN, "우회 거리의 운전자나 동승자만 확인할 수 있습니다."),
}
