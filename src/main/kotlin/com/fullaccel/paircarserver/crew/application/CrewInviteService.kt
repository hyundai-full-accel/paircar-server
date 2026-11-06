package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.crew.application.dto.InviteJoinResult
import com.fullaccel.paircarserver.crew.application.dto.InviteResult

// 초대 링크. 동승자는 앱 설치나 로그인 없이 링크로 참여함
interface CrewInviteService {

    // 크루 멤버만 발급 가능. 7일 뒤 만료
    fun createInvite(crewId: Long, requesterMemberId: Long): InviteResult

    // 참여 화면용 조회. 없으면 INVITE_NOT_FOUND, 만료됐으면 INVITE_EXPIRED
    fun getInvite(code: String): InviteResult

    // 처음 온 동승자: 멤버를 만들고 크루에 넣고 토큰을 발급함
    fun joinAsNewMember(code: String, nickname: String): InviteJoinResult

    // 이미 토큰이 있는 멤버: 크루에만 넣음
    fun joinAsMember(code: String, memberId: Long): CrewMemberResult
}
