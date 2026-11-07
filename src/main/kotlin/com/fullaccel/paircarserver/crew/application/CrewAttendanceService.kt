package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.AttendanceCommand
import com.fullaccel.paircarserver.crew.application.dto.AttendanceResult
import java.time.LocalDate

// 출석: 그날 탈지, 그날만 다른 곳에서 탈지. 기록이 없으면 "탐", 기본 탈 곳
// 차량 앱은 출발 전에 이 목록으로 오늘 태울 사람과 경유지를 정함
interface CrewAttendanceService {

    // 크루 멤버 전원의 그날 출석 (참여 순서)
    fun getAttendances(crewId: Long, serviceDate: LocalDate): List<AttendanceResult>

    // 본인이나 차를 등록한 크루 멤버만 (CREW_MEMBER_SELF_OR_DRIVER_ONLY). 오늘(KST) 이전이면 ATTENDANCE_DATE_PASSED
    // 바뀌었으면 커밋 뒤 AttendanceChangedEvent를 발행함
    fun changeAttendance(
        requesterMemberId: Long,
        crewId: Long,
        crewMemberId: Long,
        serviceDate: LocalDate,
        command: AttendanceCommand,
    ): AttendanceResult
}
