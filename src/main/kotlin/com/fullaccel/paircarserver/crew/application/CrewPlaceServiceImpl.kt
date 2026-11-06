package com.fullaccel.paircarserver.crew.application

import com.fullaccel.paircarserver.crew.application.dto.CrewPlaceResult
import com.fullaccel.paircarserver.crew.application.dto.RegisterPlaceCommand
import com.fullaccel.paircarserver.crew.domain.CrewPlace
import com.fullaccel.paircarserver.crew.domain.CrewPlaceRepository
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CrewPlaceServiceImpl(
    private val crewPlaceRepository: CrewPlaceRepository,
    private val crewService: CrewService,
) : CrewPlaceService {

    @Transactional
    override fun registerPlace(crewId: Long, command: RegisterPlaceCommand): CrewPlaceResult {
        val crew = crewService.getCrewEntity(crewId)
        if (crewPlaceRepository.existsByCrewIdAndName(crewId, command.name)) {
            throw BusinessException(ErrorCode.CREW_PLACE_NAME_DUPLICATED)
        }
        val place = CrewPlace(
            crew = crew,
            name = command.name,
            latitude = command.latitude,
            longitude = command.longitude,
        )
        return CrewPlaceResult.from(crewPlaceRepository.save(place))
    }

    override fun getPlaces(crewId: Long): List<CrewPlaceResult> {
        crewService.getCrewEntity(crewId)
        return crewPlaceRepository.findAllByCrewIdOrderById(crewId).map(CrewPlaceResult::from)
    }

    override fun getPlaceEntity(placeId: Long): CrewPlace =
        crewPlaceRepository.findByIdOrNull(placeId)
            ?: throw BusinessException(ErrorCode.CREW_PLACE_NOT_FOUND)

    override fun getPlaceEntity(crewId: Long, placeId: Long): CrewPlace {
        val place = getPlaceEntity(placeId)
        if (place.crew.id != crewId) throw BusinessException(ErrorCode.CREW_MISMATCH)
        return place
    }
}
