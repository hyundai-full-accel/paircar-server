package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.CrewPlaceResult
import com.fullaccel.paircarserver.crew.application.dto.RegisterPlaceCommand
import com.fullaccel.paircarserver.crew.domain.CrewPlace

// 크루 장소 (아파트 정문, 회사, 운전자 집). 크루 멤버 누구나 등록하고 크루 전체가 같이 씀
interface CrewPlaceService {

    // 같은 크루에 같은 이름이 있으면 CREW_PLACE_NAME_DUPLICATED
    fun registerPlace(crewId: Long, command: RegisterPlaceCommand): CrewPlaceResult

    fun getPlaces(crewId: Long): List<CrewPlaceResult>

    // 다른 패키지 서비스가 연관관계를 맺을 때 쓰는 엔티티 조회. 트랜잭션 안에서만 사용
    fun getPlaceEntity(placeId: Long): CrewPlace

    // 장소가 이 크루 것이 아니면 CREW_MISMATCH
    fun getPlaceEntity(crewId: Long, placeId: Long): CrewPlace
}
