package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.car.application.CarService
import com.fullaccel.paircarserver.crew.application.dto.CrewMemberResult
import com.fullaccel.paircarserver.crew.application.dto.CrewResult
import com.fullaccel.paircarserver.crew.application.dto.UpdateCrewCommand
import com.fullaccel.paircarserver.crew.domain.CrewMember
import com.fullaccel.paircarserver.crew.domain.SettlementMode
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CrewSettingServiceImpl(
    private val crewService: CrewService,
    private val crewPlaceService: CrewPlaceService,
    private val carService: CarService,
) : CrewSettingService {

    @Transactional
    override fun updateCrew(requesterMemberId: Long, crewId: Long, command: UpdateCrewCommand): CrewResult {
        val crew = crewService.getCrewEntity(crewId)
        if (!carService.hasCar(requesterMemberId)) throw BusinessException(ErrorCode.CREW_DRIVER_ONLY)
        if (command.settlementMode == SettlementMode.FIXED_MONTHLY) {
            throw BusinessException(ErrorCode.SETTLEMENT_MODE_NOT_SUPPORTED)
        }
        crew.update(
            name = command.name,
            departureTime = command.departureTime,
            departureDays = command.departureDays,
            originPlace = command.originPlaceId?.let { crewPlaceService.getPlaceEntity(crewId, it) },
            destinationPlace = command.destinationPlaceId?.let { crewPlaceService.getPlaceEntity(crewId, it) },
            settlementMode = command.settlementMode,
            includeDriver = command.includeDriver,
        )
        return CrewResult.from(crew)
    }

    @Transactional
    override fun changeMemberPlaces(
        requesterMemberId: Long,
        crewId: Long,
        crewMemberId: Long,
        boardPlaceId: Long?,
        alightPlaceId: Long?,
    ): CrewMemberResult {
        val crewMember = getCrewMemberOf(crewId, crewMemberId)
        authorizeSelfOrDriver(requesterMemberId, crewMember)
        crewMember.changePlaces(
            boardPlace = boardPlaceId?.let { crewPlaceService.getPlaceEntity(crewId, it) },
            alightPlace = alightPlaceId?.let { crewPlaceService.getPlaceEntity(crewId, it) },
        )
        return CrewMemberResult.from(crewMember)
    }

    private fun getCrewMemberOf(crewId: Long, crewMemberId: Long): CrewMember {
        val crewMember = crewService.getCrewMemberEntity(crewMemberId)
        if (crewMember.crew.id != crewId) throw BusinessException(ErrorCode.CREW_MISMATCH)
        return crewMember
    }

    private fun authorizeSelfOrDriver(requesterMemberId: Long, crewMember: CrewMember) {
        if (crewMember.member.id != requesterMemberId && !carService.hasCar(requesterMemberId)) {
            throw BusinessException(ErrorCode.CREW_MEMBER_SELF_OR_DRIVER_ONLY)
        }
    }
}
