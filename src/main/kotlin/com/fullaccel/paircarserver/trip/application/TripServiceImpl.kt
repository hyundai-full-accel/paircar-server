package com.fullaccel.paircarserver.trip.application

import com.fullaccel.paircarserver.car.application.CarService
import com.fullaccel.paircarserver.crew.application.CrewPlaceService
import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.crew.domain.CrewPlace
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.global.time.KST
import com.fullaccel.paircarserver.member.application.MemberService
import com.fullaccel.paircarserver.trip.application.dto.BoardingEventCommand
import com.fullaccel.paircarserver.trip.application.dto.EndTripCommand
import com.fullaccel.paircarserver.trip.application.dto.SegmentCommand
import com.fullaccel.paircarserver.trip.application.dto.SegmentResult
import com.fullaccel.paircarserver.trip.application.dto.SettlementBoarding
import com.fullaccel.paircarserver.trip.application.dto.SettlementSegment
import com.fullaccel.paircarserver.trip.application.dto.SettlementTrip
import com.fullaccel.paircarserver.trip.application.dto.StartTripCommand
import com.fullaccel.paircarserver.trip.application.dto.TripResult
import com.fullaccel.paircarserver.trip.application.event.BoardPlaceChangedEvent
import com.fullaccel.paircarserver.trip.application.event.BoardingCanceledEvent
import com.fullaccel.paircarserver.trip.application.event.RiderBoardedEvent
import com.fullaccel.paircarserver.trip.application.event.TripEndedEvent
import com.fullaccel.paircarserver.trip.application.event.TripStartedEvent
import com.fullaccel.paircarserver.trip.domain.Boarding
import com.fullaccel.paircarserver.trip.domain.BoardingRepository
import com.fullaccel.paircarserver.trip.domain.Trip
import com.fullaccel.paircarserver.trip.domain.TripRepository
import com.fullaccel.paircarserver.trip.domain.TripSegment
import com.fullaccel.paircarserver.trip.domain.TripSegmentRepository
import com.fullaccel.paircarserver.trip.domain.TripSegmentRider
import com.fullaccel.paircarserver.trip.domain.TripSegmentRiderRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class TripServiceImpl(
    private val tripRepository: TripRepository,
    private val boardingRepository: BoardingRepository,
    private val tripSegmentRepository: TripSegmentRepository,
    private val tripSegmentRiderRepository: TripSegmentRiderRepository,
    private val memberService: MemberService,
    private val carService: CarService,
    private val crewService: CrewService,
    private val crewPlaceService: CrewPlaceService,
    private val eventPublisher: ApplicationEventPublisher,
) : TripService {

    @Transactional
    override fun startTrip(driverMemberId: Long, command: StartTripCommand): TripResult {
        if (tripRepository.existsByDriverMemberIdAndEndedAtIsNull(driverMemberId)) {
            throw BusinessException(ErrorCode.TRIP_ALREADY_IN_PROGRESS)
        }
        val crewId = command.crewId
        val driver = crewService.getCrewMemberEntity(crewId, driverMemberId)
        carService.validateMemberCar(driverMemberId, command.carId)
        val car = carService.getCarEntity(command.carId)
        if (memberService.getBankAccount(driverMemberId) == null) {
            throw BusinessException(ErrorCode.BANK_ACCOUNT_REQUIRED)
        }
        val origin = crewPlaceService.getPlaceEntity(crewId, command.originPlaceId)
        val destination = crewPlaceService.getPlaceEntity(crewId, command.destinationPlaceId)

        if (command.riders.map { it.crewMemberId }.toSet().size != command.riders.size) {
            throw BusinessException(ErrorCode.BOARDING_DUPLICATED)
        }
        val riders = command.riders.map { rider ->
            val crewMember = crewService.getCrewMemberEntity(rider.crewMemberId)
            if (crewMember.crew.id != crewId) throw BusinessException(ErrorCode.CREW_MISMATCH)
            if (crewMember.id == driver.id) throw BusinessException(ErrorCode.BOARDING_DRIVER_NOT_ALLOWED)
            Triple(
                crewMember,
                crewPlaceService.getPlaceEntity(crewId, rider.boardPlaceId),
                crewPlaceService.getPlaceEntity(crewId, rider.alightPlaceId),
            )
        }
        validateWaypointCount(origin, destination, riders.flatMap { listOf(it.second, it.third) })

        val trip = tripRepository.save(
            Trip(
                driver = driver,
                car = car,
                originPlace = origin,
                destinationPlace = destination,
                serviceDate = LocalDate.ofInstant(command.startedAt, KST),
                startedAt = command.startedAt,
                startOdometerKm = command.startOdometerKm,
            ),
        )
        boardingRepository.saveAll(
            riders.map { (crewMember, boardPlace, alightPlace) ->
                Boarding(trip = trip, crewMember = crewMember, boardPlace = boardPlace, alightPlace = alightPlace)
            },
        )
        eventPublisher.publishEvent(
            TripStartedEvent(
                tripId = trip.id,
                driverMemberId = driverMemberId,
                driverNickname = driver.member.nickname,
                riderMemberIds = riders.map { (crewMember, _, _) -> crewMember.member.id },
            ),
        )
        return toResult(trip)
    }

    override fun getTrip(tripId: Long): TripResult = toResult(getTripEntity(tripId))

    @Transactional
    override fun recordBoarding(
        driverMemberId: Long,
        tripId: Long,
        crewMemberId: Long,
        command: BoardingEventCommand,
    ): TripResult {
        val trip = getTripOfDriver(driverMemberId, tripId)
        val boarding = getBoarding(tripId, crewMemberId)
        if (!boarding.isBoarded) {
            if (command.odometerKm < trip.startOdometerKm) throw BusinessException(ErrorCode.INVALID_ODOMETER)
            boarding.board(command.occurredAt, command.odometerKm)
            eventPublisher.publishEvent(
                RiderBoardedEvent(
                    tripId = tripId,
                    driverMemberId = driverMemberId,
                    riderMemberId = boarding.crewMember.member.id,
                    riderCrewMemberId = crewMemberId,
                    riderNickname = boarding.crewMember.member.nickname,
                ),
            )
        }
        return toResult(trip)
    }

    @Transactional
    override fun recordAlighting(
        driverMemberId: Long,
        tripId: Long,
        crewMemberId: Long,
        command: BoardingEventCommand,
    ): TripResult {
        val trip = getTripOfDriver(driverMemberId, tripId)
        val boarding = getBoarding(tripId, crewMemberId)
        if (!boarding.isBoarded) throw BusinessException(ErrorCode.BOARDING_NOT_BOARDED)
        if (!boarding.isAlighted) {
            if (command.odometerKm < boarding.boardOdometerKm!!) throw BusinessException(ErrorCode.INVALID_ODOMETER)
            boarding.alight(command.occurredAt, command.odometerKm)
        }
        return toResult(trip)
    }

    @Transactional
    override fun changeBoardPlace(requesterMemberId: Long, tripId: Long, crewMemberId: Long, placeId: Long): TripResult {
        val trip = getTripEntity(tripId)
        val boarding = getBoarding(tripId, crewMemberId)
        val byRider = authorizeDriverOrRider(requesterMemberId, trip, boarding)
        if (trip.isEnded) throw BusinessException(ErrorCode.TRIP_ALREADY_ENDED)
        if (boarding.isBoarded) throw BusinessException(ErrorCode.BOARDING_ALREADY_BOARDED)

        val place = crewPlaceService.getPlaceEntity(trip.driver.crew.id, placeId)
        boarding.changeBoardPlace(place)
        val places = boardingRepository.findAllByTripIdOrderById(tripId).flatMap { listOf(it.boardPlace, it.alightPlace) }
        validateWaypointCount(trip.originPlace, trip.destinationPlace, places)

        eventPublisher.publishEvent(
            BoardPlaceChangedEvent(
                tripId = tripId,
                driverMemberId = trip.driver.member.id,
                riderCrewMemberId = crewMemberId,
                riderNickname = boarding.crewMember.member.nickname,
                placeId = place.id,
                placeName = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                byRider = byRider,
            ),
        )
        return toResult(trip)
    }

    @Transactional
    override fun cancelBoarding(requesterMemberId: Long, tripId: Long, crewMemberId: Long): TripResult {
        val trip = getTripEntity(tripId)
        val boarding = getBoarding(tripId, crewMemberId)
        val byRider = authorizeDriverOrRider(requesterMemberId, trip, boarding)
        if (trip.isEnded) throw BusinessException(ErrorCode.TRIP_ALREADY_ENDED)
        if (boarding.isBoarded) throw BusinessException(ErrorCode.BOARDING_ALREADY_BOARDED)

        boardingRepository.delete(boarding)
        eventPublisher.publishEvent(
            BoardingCanceledEvent(
                tripId = tripId,
                driverMemberId = trip.driver.member.id,
                riderCrewMemberId = crewMemberId,
                riderNickname = boarding.crewMember.member.nickname,
                byRider = byRider,
            ),
        )
        return toResult(trip)
    }

    @Transactional
    override fun uploadSegments(driverMemberId: Long, tripId: Long, segments: List<SegmentCommand>): TripResult {
        val trip = getTripOfDriver(driverMemberId, tripId)
        if (segments.map { it.seq }.toSet().size != segments.size) throw BusinessException(ErrorCode.INVALID_SEGMENT)

        val existing = tripSegmentRepository.findAllByTripIdOrderBySeq(tripId)
        val existingSeqs = existing.map { it.seq.toInt() }.toSet()
        val boardingsByCrewMember = boardingRepository.findAllByTripIdOrderById(tripId).associateBy { it.crewMember.id }
        val newSegments = segments.filter { it.seq !in existingSeqs }.sortedBy { it.seq }

        newSegments.forEach { validateSegment(trip, it, boardingsByCrewMember) }
        validateSegmentContinuity(
            existing.map { Triple(it.seq.toInt(), it.startOdometerKm, it.endOdometerKm) } +
                newSegments.map { Triple(it.seq, it.startOdometerKm, it.endOdometerKm) },
        )

        newSegments.forEach { command ->
            val segment = tripSegmentRepository.save(
                TripSegment(
                    trip = trip,
                    seq = command.seq.toShort(),
                    startOdometerKm = command.startOdometerKm,
                    endOdometerKm = command.endOdometerKm,
                    fuelEconomy = command.fuelEconomy,
                    fuelEconomySource = command.fuelEconomySource,
                    startPointIndex = command.startPointIndex,
                    endPointIndex = command.endPointIndex,
                ),
            )
            tripSegmentRiderRepository.saveAll(
                command.riderCrewMemberIds.map { TripSegmentRider(tripSegment = segment, boarding = boardingsByCrewMember.getValue(it)) },
            )
        }
        return toResult(trip)
    }

    @Transactional
    override fun endTrip(driverMemberId: Long, tripId: Long, command: EndTripCommand): TripResult {
        val trip = getTripOfDriver(driverMemberId, tripId)
        if (trip.isEnded) return toResult(trip)
        if (command.endOdometerKm < trip.startOdometerKm) throw BusinessException(ErrorCode.INVALID_ODOMETER)

        trip.end(command.endedAt, command.endOdometerKm, command.tollFareWon, command.routePolyline)
        val boardings = boardingRepository.findAllByTripIdOrderById(tripId)
        boardings
            .filter { it.isBoarded && !it.isAlighted }
            .forEach { it.alight(command.endedAt, command.endOdometerKm) }
        eventPublisher.publishEvent(TripEndedEvent(tripId = tripId, riderMemberIds = boardings.map { it.crewMember.member.id }))
        return toResult(trip)
    }

    override fun getSettlementTrips(crewId: Long, until: LocalDate): List<SettlementTrip> {
        val trips = tripRepository.findAllByDriverCrewIdAndEndedAtIsNotNullAndServiceDateBeforeOrderByServiceDateAscIdAsc(crewId, until)
        if (trips.isEmpty()) return emptyList()
        val tripIds = trips.map { it.id }
        val segmentsByTrip = tripSegmentRepository.findAllByTripIdIn(tripIds).groupBy { it.trip.id }
        val ridersBySegment = tripSegmentRiderRepository.findAllByTripSegmentTripIdIn(tripIds)
            .groupBy({ it.tripSegment.id }, { it.boarding.id })
        val boardingsByTrip = boardingRepository.findAllByTripIdInAndBoardedAtIsNotNull(tripIds).groupBy { it.trip.id }

        return trips
            .filter { segmentsByTrip.containsKey(it.id) } // 구간이 아직 안 올라온 운행은 다음 정산으로 미룸
            .map { trip ->
                SettlementTrip(
                    tripId = trip.id,
                    driverCrewMemberId = trip.driver.id,
                    fuelType = trip.car.fuelType,
                    serviceDate = trip.serviceDate,
                    originPlaceId = trip.originPlace.id,
                    destinationPlaceId = trip.destinationPlace.id,
                    tollFareWon = trip.tollFareWon,
                    segments = segmentsByTrip.getValue(trip.id).sortedBy { it.seq }.map { segment ->
                        SettlementSegment(
                            distanceKm = segment.endOdometerKm - segment.startOdometerKm,
                            fuelEconomy = segment.fuelEconomy,
                            fuelEconomySource = segment.fuelEconomySource,
                            riderBoardingIds = ridersBySegment[segment.id].orEmpty().toSet(),
                        )
                    },
                    boardings = boardingsByTrip[trip.id].orEmpty().map { boarding ->
                        SettlementBoarding(
                            boardingId = boarding.id,
                            crewMemberId = boarding.crewMember.id,
                            boardPlaceId = boarding.boardPlace.id,
                            alightPlaceId = boarding.alightPlace.id,
                        )
                    },
                )
            }
    }

    override fun getBoardingEntity(boardingId: Long): Boarding =
        boardingRepository.findByIdOrNull(boardingId) ?: throw BusinessException(ErrorCode.BOARDING_NOT_FOUND)

    private fun getTripEntity(tripId: Long): Trip =
        tripRepository.findByIdOrNull(tripId) ?: throw BusinessException(ErrorCode.TRIP_NOT_FOUND)

    private fun getTripOfDriver(driverMemberId: Long, tripId: Long): Trip {
        val trip = getTripEntity(tripId)
        if (trip.driver.member.id != driverMemberId) throw BusinessException(ErrorCode.TRIP_ACCESS_DENIED)
        return trip
    }

    // 운전자나 그 탑승의 동승자 본인만 허용. 동승자 본인이면 true
    private fun authorizeDriverOrRider(requesterMemberId: Long, trip: Trip, boarding: Boarding): Boolean =
        when (requesterMemberId) {
            trip.driver.member.id -> false
            boarding.crewMember.member.id -> true
            else -> throw BusinessException(ErrorCode.TRIP_ACCESS_DENIED)
        }

    private fun getBoarding(tripId: Long, crewMemberId: Long): Boarding =
        boardingRepository.findByTripIdAndCrewMemberId(tripId, crewMemberId)
            ?: throw BusinessException(ErrorCode.BOARDING_NOT_FOUND)

    // 규칙 7: 출발지, 목적지를 뺀 경유지는 최대 2곳 (기술명세 T8)
    private fun validateWaypointCount(origin: CrewPlace, destination: CrewPlace, places: List<CrewPlace>) {
        val endpoints = setOf(origin.id, destination.id)
        val waypointCount = places.map { it.id }.filter { it !in endpoints }.toSet().size
        if (waypointCount > MAX_WAYPOINTS) throw BusinessException(ErrorCode.TOO_MANY_WAYPOINTS)
    }

    private fun validateSegment(trip: Trip, command: SegmentCommand, boardingsByCrewMember: Map<Long, Boarding>) {
        val valid = command.seq >= 1 &&
            command.startOdometerKm >= trip.startOdometerKm &&
            command.endOdometerKm >= command.startOdometerKm &&
            command.fuelEconomy > BigDecimal.ZERO &&
            command.riderCrewMemberIds.toSet().size == command.riderCrewMemberIds.size &&
            command.riderCrewMemberIds.all { boardingsByCrewMember[it]?.isBoarded == true }
        if (!valid) throw BusinessException(ErrorCode.INVALID_SEGMENT)
    }

    // 규칙 8: 이어지는 seq끼리는 앞 구간 끝 = 다음 구간 시작
    private fun validateSegmentContinuity(segments: List<Triple<Int, BigDecimal, BigDecimal>>) {
        segments.sortedBy { it.first }.zipWithNext().forEach { (prev, next) ->
            if (next.first == prev.first + 1 && prev.third.compareTo(next.second) != 0) {
                throw BusinessException(ErrorCode.INVALID_SEGMENT)
            }
        }
    }

    private fun toResult(trip: Trip): TripResult {
        val ridersBySegment = tripSegmentRiderRepository.findAllByTripSegmentTripId(trip.id)
            .groupBy({ it.tripSegment.id }, { it.boarding.crewMember.id })
        val segments = tripSegmentRepository.findAllByTripIdOrderBySeq(trip.id)
            .map { SegmentResult.of(it, ridersBySegment[it.id].orEmpty().sorted()) }
        return TripResult.of(trip, boardingRepository.findAllByTripIdOrderById(trip.id), segments)
    }

    companion object {
        private const val MAX_WAYPOINTS = 2
    }
}
