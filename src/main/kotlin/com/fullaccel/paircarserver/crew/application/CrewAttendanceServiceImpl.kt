package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.car.application.CarService
import com.fullaccel.paircarserver.crew.application.dto.AttendanceCommand
import com.fullaccel.paircarserver.crew.application.dto.AttendanceResult
import com.fullaccel.paircarserver.crew.application.dto.CrewPlaceResult
import com.fullaccel.paircarserver.crew.application.event.AttendanceChangedEvent
import com.fullaccel.paircarserver.crew.application.event.AttendancePlace
import com.fullaccel.paircarserver.crew.domain.CrewAttendance
import com.fullaccel.paircarserver.crew.domain.CrewAttendanceRepository
import com.fullaccel.paircarserver.crew.domain.CrewMember
import com.fullaccel.paircarserver.crew.domain.CrewMemberRepository
import com.fullaccel.paircarserver.crew.domain.CrewPlace
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CrewAttendanceServiceImpl(
    private val crewAttendanceRepository: CrewAttendanceRepository,
    private val crewMemberRepository: CrewMemberRepository,
    private val crewService: CrewService,
    private val crewPlaceService: CrewPlaceService,
    private val carService: CarService,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) : CrewAttendanceService {

    override fun getAttendances(crewId: Long, serviceDate: LocalDate): List<AttendanceResult> {
        crewService.getCrewEntity(crewId)
        val attendances = crewAttendanceRepository.findAllByCrewMemberCrewIdAndServiceDate(crewId, serviceDate)
            .associateBy { it.crewMember.id }
        return crewMemberRepository.findAllByCrewIdOrderById(crewId).map { toResult(it, serviceDate, attendances[it.id]) }
    }

    @Transactional
    override fun changeAttendance(
        requesterMemberId: Long,
        crewId: Long,
        crewMemberId: Long,
        serviceDate: LocalDate,
        command: AttendanceCommand,
    ): AttendanceResult {
        val crewMember = crewService.getCrewMemberEntity(crewMemberId)
        if (crewMember.crew.id != crewId) throw BusinessException(ErrorCode.CREW_MISMATCH)
        if (crewMember.member.id != requesterMemberId && !carService.hasCar(requesterMemberId)) {
            throw BusinessException(ErrorCode.CREW_MEMBER_SELF_OR_DRIVER_ONLY)
        }
        if (serviceDate.isBefore(LocalDate.now(clock))) throw BusinessException(ErrorCode.ATTENDANCE_DATE_PASSED)
        val boardPlace = command.boardPlaceId?.let { crewPlaceService.getPlaceEntity(crewId, it) }

        val existing = crewAttendanceRepository.findByCrewMemberIdAndServiceDate(crewMemberId, serviceDate)
        val delayMinutes = command.delayMinutes.takeIf { command.attending }
        val changed = existing?.change(command.attending, boardPlace, delayMinutes) ?: true
        val attendance = existing
            ?: crewAttendanceRepository.save(CrewAttendance(crewMember, serviceDate, command.attending, boardPlace, delayMinutes))
        if (changed) publishChanged(crewMember, serviceDate, command.attending, boardPlace ?: crewMember.boardPlace, delayMinutes)
        return toResult(crewMember, serviceDate, attendance)
    }

    private fun publishChanged(crewMember: CrewMember, serviceDate: LocalDate, attending: Boolean, boardPlace: CrewPlace?, delayMinutes: Int?) {
        val recipients = crewMemberRepository.findAllByCrewIdOrderById(crewMember.crew.id)
            .map { it.member.id }
            .filter { it != crewMember.member.id }
        eventPublisher.publishEvent(
            AttendanceChangedEvent(
                crewId = crewMember.crew.id,
                crewMemberId = crewMember.id,
                nickname = crewMember.member.nickname,
                serviceDate = serviceDate,
                attending = attending,
                boardPlace = boardPlace?.let { AttendancePlace(it.id, it.name, it.latitude, it.longitude) },
                delayMinutes = delayMinutes,
                recipientMemberIds = recipients,
            ),
        )
    }

    private fun toResult(crewMember: CrewMember, serviceDate: LocalDate, attendance: CrewAttendance?) = AttendanceResult(
        crewMemberId = crewMember.id,
        memberId = crewMember.member.id,
        nickname = crewMember.member.nickname,
        serviceDate = serviceDate,
        attending = attendance?.attending ?: true,
        boardPlace = (attendance?.boardPlace ?: crewMember.boardPlace)?.let(CrewPlaceResult::from),
        alightPlace = crewMember.alightPlace?.let(CrewPlaceResult::from),
        delayMinutes = attendance?.delayMinutes,
        changed = attendance != null,
    )
}
