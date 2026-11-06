package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.DetourResult
import com.fullaccel.paircarserver.crew.application.dto.RegisterDetourCommand
import com.fullaccel.paircarserver.crew.domain.CrewMemberDetour
import com.fullaccel.paircarserver.crew.domain.CrewMemberDetourRepository
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CrewDetourServiceImpl(
    private val crewMemberDetourRepository: CrewMemberDetourRepository,
    private val crewService: CrewService,
    private val crewPlaceService: CrewPlaceService,
) : CrewDetourService {

    @Transactional
    override fun registerDetour(crewId: Long, command: RegisterDetourCommand): DetourResult {
        crewService.getCrewEntity(crewId)
        if (command.passengerId == command.driverId) {
            throw BusinessException(ErrorCode.DETOUR_SAME_PASSENGER_AND_DRIVER)
        }
        val passenger = crewService.getCrewMemberEntity(command.passengerId)
        val driver = crewService.getCrewMemberEntity(command.driverId)
        if (passenger.crew.id != crewId || driver.crew.id != crewId) {
            throw BusinessException(ErrorCode.CREW_MISMATCH)
        }
        val place = crewPlaceService.getPlaceEntity(crewId, command.placeId)

        val existing = crewMemberDetourRepository.findByPassengerIdAndDriverIdAndPlaceId(passenger.id, driver.id, place.id)
        if (existing != null) {
            existing.changeDetourKm(command.detourKm)
            return DetourResult.from(existing)
        }
        val detour = CrewMemberDetour(
            passenger = passenger,
            driver = driver,
            place = place,
            detourKm = command.detourKm,
        )
        return DetourResult.from(crewMemberDetourRepository.save(detour))
    }

    @Transactional
    override fun confirmDetour(detourId: Long, confirmerCrewMemberId: Long): DetourResult {
        val detour = crewMemberDetourRepository.findByIdOrNull(detourId)
            ?: throw BusinessException(ErrorCode.DETOUR_NOT_FOUND)
        if (!detour.confirmBy(confirmerCrewMemberId)) {
            throw BusinessException(ErrorCode.DETOUR_CONFIRMER_NOT_PARTY)
        }
        return DetourResult.from(detour)
    }

    override fun getDetours(crewId: Long): List<DetourResult> {
        crewService.getCrewEntity(crewId)
        return crewMemberDetourRepository.findAllByPassengerCrewIdOrderById(crewId).map(DetourResult::from)
    }

    override fun findDetour(passengerId: Long, driverId: Long, placeId: Long): DetourResult? =
        crewMemberDetourRepository.findByPassengerIdAndDriverIdAndPlaceId(passengerId, driverId, placeId)
            ?.let(DetourResult::from)
}
