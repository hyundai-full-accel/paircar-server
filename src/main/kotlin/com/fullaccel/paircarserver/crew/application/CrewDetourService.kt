package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.DetourResult
import com.fullaccel.paircarserver.crew.application.dto.RegisterDetourCommand

// 우회 거리 (기술명세 T4). 크루 합류 때 1번 산출하고 운전자, 동승자가 확인한 값을 정산에 씀
interface CrewDetourService {

    // 같은 동승자, 운전자, 장소가 이미 있으면 값을 바꾸고 양측 확인을 초기화함
    // 새로 등록되거나 값이 바뀌면 DetourConfirmationRequestedEvent를 발행함
    // 동승자, 운전자, 장소가 같은 크루가 아니면 CREW_MISMATCH, 동승자와 운전자가 같으면 DETOUR_SAME_PASSENGER_AND_DRIVER
    fun registerDetour(crewId: Long, command: RegisterDetourCommand): DetourResult

    // 운전자나 동승자 본인이 확인. 둘 다 확인하면 confirmed
    fun confirmDetour(detourId: Long, confirmerCrewMemberId: Long): DetourResult

    fun getDetours(crewId: Long): List<DetourResult>

    // 정산, 운행에서 쓰는 조회. 없으면 null
    fun findDetour(passengerId: Long, driverId: Long, placeId: Long): DetourResult?
}
