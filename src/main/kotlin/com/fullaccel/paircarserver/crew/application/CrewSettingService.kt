package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.crew.application.dto.CrewResult
import com.fullaccel.paircarserver.crew.application.dto.UpdateCrewCommand

// 크루 설정(이름, 기본 일정, 정산 방식)과 크루 멤버의 기본 탈 곳, 내릴 곳
// 크루에 역할이 없으므로 "차를 등록한 멤버"를 운전할 수 있는 사람으로 보고 권한을 나눔
interface CrewSettingService {

    // 차를 등록한 크루 멤버만 (CREW_DRIVER_ONLY). 정산 방식은 내는 사람이 정하지 않게 하려는 것
    // 장소는 이 크루 것이어야 함 (CREW_MISMATCH). FIXED_MONTHLY는 아직 정산하지 못하므로 SETTLEMENT_MODE_NOT_SUPPORTED
    fun updateCrew(requesterMemberId: Long, crewId: Long, command: UpdateCrewCommand): CrewResult

    // 본인이나 차를 등록한 크루 멤버만 (CREW_MEMBER_SELF_OR_DRIVER_ONLY). null은 지움
    fun changeMemberPlaces(
        requesterMemberId: Long,
        crewId: Long,
        crewMemberId: Long,
        boardPlaceId: Long?,
        alightPlaceId: Long?,
    ): CrewMemberResult
}
