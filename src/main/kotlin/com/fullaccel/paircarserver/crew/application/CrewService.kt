package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.crew.application.dto.CrewResult
import com.fullaccel.paircarserver.crew.domain.Crew
import com.fullaccel.paircarserver.crew.domain.CrewMember

// 크루와 크루 멤버
interface CrewService {

    // 만든 멤버는 자동으로 크루에 참여함
    fun createCrew(creatorMemberId: Long, name: String): CrewResult

    fun getCrew(crewId: Long): CrewResult

    // 1차는 초대 링크 없이 크루에 바로 추가. 이미 참여했으면 CREW_MEMBER_ALREADY_EXISTS
    fun addMember(crewId: Long, memberId: Long): CrewMemberResult

    fun getCrewMembers(crewId: Long): List<CrewMemberResult>

    // 월 자동 정산 대상. 모든 크루의 ID
    fun getAllCrewIds(): List<Long>

    // 멤버가 참여한 크루 목록
    fun getCrewsOfMember(memberId: Long): List<CrewResult>

    // API 접근 검증: 멤버가 이 크루에 없으면 CREW_ACCESS_DENIED. 있으면 그 크루 멤버 ID를 반환
    fun validateCrewAccess(crewId: Long, memberId: Long): Long

    // 다른 패키지 서비스가 연관관계를 맺을 때 쓰는 엔티티 조회. 트랜잭션 안에서만 사용
    fun getCrewEntity(crewId: Long): Crew

    fun getCrewMemberEntity(crewMemberId: Long): CrewMember

    // 멤버가 이 크루에 없으면 CREW_MEMBER_NOT_FOUND
    fun getCrewMemberEntity(crewId: Long, memberId: Long): CrewMember
}
