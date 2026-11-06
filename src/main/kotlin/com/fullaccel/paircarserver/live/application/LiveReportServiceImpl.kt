package com.fullaccel.paircarserver.live.application

import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.live.application.dto.DriverDelayData
import com.fullaccel.paircarserver.live.application.dto.LiveEvent
import com.fullaccel.paircarserver.live.application.dto.LiveEventType
import com.fullaccel.paircarserver.live.application.dto.RiderData
import com.fullaccel.paircarserver.live.application.dto.RiderDelayData
import com.fullaccel.paircarserver.live.application.dto.VehicleLocationCommand
import com.fullaccel.paircarserver.live.application.dto.VehicleLocationData
import com.fullaccel.paircarserver.trip.application.TripService
import com.fullaccel.paircarserver.trip.application.dto.BoardingResult
import com.fullaccel.paircarserver.trip.application.dto.TripResult
import org.springframework.stereotype.Service

@Service
class LiveReportServiceImpl(
    private val tripService: TripService,
    private val liveNotifier: LiveNotifier,
) : LiveReportService {

    override fun reportVehicleLocation(driverMemberId: Long, tripId: Long, command: VehicleLocationCommand) {
        val trip = getOngoingTripOfDriver(driverMemberId, tripId)
        val remains = command.remainsByRider.associateBy { it.crewMemberId }
        trip.boardings
            .filter { it.alightedAt == null }
            .forEach { rider ->
                val remain = remains[rider.crewMemberId]
                val data = VehicleLocationData(command.latitude, command.longitude, remain?.remainSeconds, remain?.remainMeters)
                liveNotifier.notify(listOf(rider.memberId), LiveEvent(LiveEventType.VEHICLE_LOCATION, tripId, data))
            }
    }

    override fun reportApproaching(driverMemberId: Long, tripId: Long, riderCrewMemberId: Long) =
        notifyRider(driverMemberId, tripId, riderCrewMemberId, LiveEventType.VEHICLE_APPROACHING)

    override fun reportArrived(driverMemberId: Long, tripId: Long, riderCrewMemberId: Long) =
        notifyRider(driverMemberId, tripId, riderCrewMemberId, LiveEventType.VEHICLE_ARRIVED)

    override fun notifyDriverDelay(driverMemberId: Long, tripId: Long, minutes: Int) {
        val trip = getOngoingTripOfDriver(driverMemberId, tripId)
        liveNotifier.notify(
            trip.boardings.filter { it.boardedAt == null }.map { it.memberId },
            LiveEvent(LiveEventType.DRIVER_DELAYED, tripId, DriverDelayData(trip.driverNickname, minutes)),
        )
    }

    override fun notifyRiderDelay(riderMemberId: Long, tripId: Long, minutes: Int) {
        val (trip, rider) = getOngoingTripOfRider(riderMemberId, tripId)
        liveNotifier.notify(
            listOf(trip.driverMemberId),
            LiveEvent(LiveEventType.RIDER_DELAYED, tripId, RiderDelayData(rider.crewMemberId, rider.nickname, minutes)),
        )
    }

    override fun notifyRiderHere(riderMemberId: Long, tripId: Long) {
        val (trip, rider) = getOngoingTripOfRider(riderMemberId, tripId)
        liveNotifier.notify(
            listOf(trip.driverMemberId),
            LiveEvent(LiveEventType.RIDER_HERE, tripId, RiderData(rider.crewMemberId, rider.nickname)),
        )
    }

    private fun notifyRider(driverMemberId: Long, tripId: Long, riderCrewMemberId: Long, type: LiveEventType) {
        val trip = getOngoingTripOfDriver(driverMemberId, tripId)
        val rider = trip.boardings.firstOrNull { it.crewMemberId == riderCrewMemberId }
            ?: throw BusinessException(ErrorCode.BOARDING_NOT_FOUND)
        liveNotifier.notify(listOf(rider.memberId), LiveEvent(type, tripId, RiderData(rider.crewMemberId, rider.nickname)))
    }

    private fun getOngoingTripOfDriver(driverMemberId: Long, tripId: Long): TripResult {
        val trip = getOngoingTrip(tripId)
        if (trip.driverMemberId != driverMemberId) throw BusinessException(ErrorCode.TRIP_ACCESS_DENIED)
        return trip
    }

    private fun getOngoingTripOfRider(riderMemberId: Long, tripId: Long): Pair<TripResult, BoardingResult> {
        val trip = getOngoingTrip(tripId)
        val rider = trip.boardings.firstOrNull { it.memberId == riderMemberId }
            ?: throw BusinessException(ErrorCode.TRIP_ACCESS_DENIED)
        return trip to rider
    }

    private fun getOngoingTrip(tripId: Long): TripResult {
        val trip = tripService.getTrip(tripId)
        if (trip.endedAt != null) throw BusinessException(ErrorCode.TRIP_ALREADY_ENDED)
        return trip
    }
}
