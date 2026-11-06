package com.fullaccel.paircarserver.settlement.application

import com.fullaccel.paircarserver.car.domain.FuelType
import com.fullaccel.paircarserver.crew.application.CrewDetourService
import com.fullaccel.paircarserver.crew.application.CrewService
import com.fullaccel.paircarserver.crew.domain.Crew
import com.fullaccel.paircarserver.crew.domain.SettlementMode
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import com.fullaccel.paircarserver.settlement.application.dto.ReceiptResult
import com.fullaccel.paircarserver.settlement.application.event.ReceiptIssuedEvent
import com.fullaccel.paircarserver.settlement.calculator.EqualRideInput
import com.fullaccel.paircarserver.settlement.calculator.FairRideInput
import com.fullaccel.paircarserver.settlement.calculator.RideCost
import com.fullaccel.paircarserver.settlement.calculator.SegmentPortion
import com.fullaccel.paircarserver.settlement.calculator.SettlementCalculator
import com.fullaccel.paircarserver.settlement.domain.FuelPriceRepository
import com.fullaccel.paircarserver.settlement.domain.Receipt
import com.fullaccel.paircarserver.settlement.domain.ReceiptItem
import com.fullaccel.paircarserver.settlement.domain.ReceiptItemRepository
import com.fullaccel.paircarserver.settlement.domain.ReceiptRepository
import com.fullaccel.paircarserver.trip.application.TripService
import com.fullaccel.paircarserver.trip.application.dto.SettlementBoarding
import com.fullaccel.paircarserver.trip.application.dto.SettlementTrip
import com.fullaccel.paircarserver.trip.domain.FuelEconomySource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

@Service
@Transactional(readOnly = true)
class SettlementServiceImpl(
    private val receiptRepository: ReceiptRepository,
    private val receiptItemRepository: ReceiptItemRepository,
    private val fuelPriceRepository: FuelPriceRepository,
    private val tripService: TripService,
    private val crewService: CrewService,
    private val crewDetourService: CrewDetourService,
    private val memberService: MemberService,
    private val eventPublisher: ApplicationEventPublisher,
) : SettlementService {

    @Transactional
    override fun issueReceipts(crewId: Long, month: YearMonth): List<ReceiptResult> {
        val crew = crewService.getCrewEntity(crewId)
        if (crew.settlementMode == SettlementMode.FIXED_MONTHLY) {
            throw BusinessException(ErrorCode.SETTLEMENT_MODE_NOT_SUPPORTED)
        }
        val monthStart = month.atDay(1)
        val trips = tripService.getSettlementTrips(crewId, until = month.plusMonths(1).atDay(1))
        val settledBoardingIds = receiptItemRepository
            .findAllByBoardingIdIn(trips.flatMap { trip -> trip.boardings.map { it.boardingId } })
            .map { it.boarding.id }
            .toSet()

        val fuelPrices = mutableMapOf<FuelType, Int>()
        val rides = trips.flatMap { trip ->
            val fuelPriceWon = fuelPrices.getOrPut(trip.fuelType) { findFuelPriceWon(crew, trip.fuelType, monthStart) }
            trip.boardings
                .filter { it.boardingId !in settledBoardingIds }
                .map { boarding -> calculateRide(crew, trip, boarding, fuelPriceWon) }
        }

        return rides
            .groupBy { it.payerId to it.payeeId }
            .filterKeys { (payerId, payeeId) -> !receiptRepository.existsByPayerIdAndPayeeIdAndMonth(payerId, payeeId, monthStart) }
            .map { (pair, pairRides) -> issueReceipt(crew, pair.first, pair.second, monthStart, pairRides) }
    }

    override fun getReceipt(receiptId: Long): ReceiptResult {
        val receipt = receiptRepository.findByIdOrNull(receiptId)
            ?: throw BusinessException(ErrorCode.RECEIPT_NOT_FOUND)
        return ReceiptResult.of(receipt, receiptItemRepository.findAllByReceiptIdInOrderById(listOf(receipt.id)))
    }

    override fun getReceipts(crewId: Long, month: YearMonth): List<ReceiptResult> {
        crewService.getCrewEntity(crewId)
        val receipts = receiptRepository.findAllByPayerCrewIdAndMonthOrderById(crewId, month.atDay(1))
        val itemsByReceipt = receiptItemRepository.findAllByReceiptIdInOrderById(receipts.map { it.id }).groupBy { it.receipt.id }
        return receipts.map { ReceiptResult.of(it, itemsByReceipt[it.id].orEmpty()) }
    }

    private fun calculateRide(crew: Crew, trip: SettlementTrip, boarding: SettlementBoarding, fuelPriceWon: Int): Ride {
        val driverHeadCount = if (crew.includeDriver) 1 else 0
        val tripHeadCount = trip.boardings.size + driverHeadCount
        val tollShare = if (tripHeadCount == 0) {
            BigDecimal.ZERO
        } else {
            trip.tollFareWon.toBigDecimal().divide(tripHeadCount.toBigDecimal(), 2, RoundingMode.HALF_UP)
        }
        val riderSegments = trip.segments.filter { boarding.boardingId in it.riderBoardingIds }

        val cost = when (crew.settlementMode) {
            SettlementMode.FAIR -> SettlementCalculator.fairRide(
                FairRideInput(
                    segments = riderSegments.map {
                        SegmentPortion(it.distanceKm, it.fuelEconomy, headCount = it.riderBoardingIds.size + driverHeadCount)
                    },
                    detourKm = confirmedDetourKm(trip, boarding),
                    detourFuelEconomy = tripFuelEconomy(trip),
                    fuelPriceWon = fuelPriceWon,
                    tollShareWon = tollShare,
                ),
            )
            SettlementMode.EQUAL -> SettlementCalculator.equalRide(
                EqualRideInput(
                    tripSegments = trip.segments.map { SegmentPortion(it.distanceKm, it.fuelEconomy) },
                    headCount = tripHeadCount,
                    fuelPriceWon = fuelPriceWon,
                    tollShareWon = tollShare,
                ),
            )
            SettlementMode.FIXED_MONTHLY -> throw BusinessException(ErrorCode.SETTLEMENT_MODE_NOT_SUPPORTED)
        }
        val usedSegments = if (crew.settlementMode == SettlementMode.EQUAL) trip.segments else riderSegments
        return Ride(
            payerId = boarding.crewMemberId,
            payeeId = trip.driverCrewMemberId,
            boardingId = boarding.boardingId,
            cost = cost,
            manualFuelEconomy = usedSegments.any { it.fuelEconomySource == FuelEconomySource.MANUAL },
        )
    }

    // 출발지, 목적지가 아닌 탈 지점, 내릴 지점(경유지)의 우회 거리. 운전자, 동승자가 둘 다 확인한 값만 씀
    private fun confirmedDetourKm(trip: SettlementTrip, boarding: SettlementBoarding): BigDecimal =
        setOf(boarding.boardPlaceId, boarding.alightPlaceId)
            .filter { it != trip.originPlaceId && it != trip.destinationPlaceId }
            .mapNotNull { placeId -> crewDetourService.findDetour(boarding.crewMemberId, trip.driverCrewMemberId, placeId) }
            .filter { it.confirmed }
            .fold(BigDecimal.ZERO) { sum, detour -> sum + detour.detourKm }

    // 우회 거리에 적용할 운행 평균 연비 = 운행 거리 ÷ 운행 연료 사용량
    private fun tripFuelEconomy(trip: SettlementTrip): BigDecimal {
        val distance = trip.segments.fold(BigDecimal.ZERO) { sum, it -> sum + it.distanceKm }
        val fuel = trip.segments.fold(BigDecimal.ZERO) { sum, it -> sum + it.distanceKm.divide(it.fuelEconomy, 10, RoundingMode.HALF_UP) }
        if (fuel.signum() == 0) return trip.segments.first().fuelEconomy
        return distance.divide(fuel, 10, RoundingMode.HALF_UP)
    }

    // 크루가 직접 넣은 유가가 우선. 없으면 연료별 월평균 유가 (하이브리드는 휘발유 기준)
    private fun findFuelPriceWon(crew: Crew, fuelType: FuelType, month: LocalDate): Int {
        crew.manualFuelPriceWon?.let { return it }
        val priceFuelType = if (fuelType == FuelType.HYBRID) FuelType.GASOLINE else fuelType
        return fuelPriceRepository.findByFuelTypeAndMonth(priceFuelType, month)?.priceWon
            ?: throw BusinessException(ErrorCode.FUEL_PRICE_NOT_FOUND)
    }

    private fun issueReceipt(crew: Crew, payerId: Long, payeeId: Long, month: LocalDate, rides: List<Ride>): ReceiptResult {
        val payer = crewService.getCrewMemberEntity(payerId)
        val payee = crewService.getCrewMemberEntity(payeeId)
        val account = memberService.getBankAccount(payee.member.id)
            ?: throw BusinessException(ErrorCode.BANK_ACCOUNT_REQUIRED)
        val costs = rides.map { it.cost }

        val receipt = receiptRepository.save(
            Receipt(
                payer = payer,
                payee = payee,
                month = month,
                settlementMode = crew.settlementMode,
                fuelEconomy = SettlementCalculator.averageFuelEconomy(costs),
                fuelEconomySource = if (rides.any { it.manualFuelEconomy }) FuelEconomySource.MANUAL else FuelEconomySource.MEASURED,
                fuelPriceWon = SettlementCalculator.averageFuelPriceWon(costs),
                amountWon = SettlementCalculator.monthlyAmountWon(costs),
                payeeBankName = account.bankName,
                payeeAccountNumber = account.accountNumber,
                payeeAccountHolder = account.accountHolder,
            ),
        )
        val items = receiptItemRepository.saveAll(
            rides.map { ride ->
                ReceiptItem(
                    receipt = receipt,
                    boarding = tripService.getBoardingEntity(ride.boardingId),
                    sharedDistanceKm = ride.cost.sharedDistanceKm,
                    sharedCostWon = ride.cost.sharedCostWon,
                    detourKm = ride.cost.detourKm,
                    detourCostWon = ride.cost.detourCostWon,
                    tollWon = ride.cost.tollWon,
                )
            },
        )
        eventPublisher.publishEvent(
            ReceiptIssuedEvent(
                receiptId = receipt.id,
                month = YearMonth.from(month),
                payerMemberId = payer.member.id,
                payeeNickname = payee.member.nickname,
                amountWon = receipt.amountWon,
            ),
        )
        return ReceiptResult.of(receipt, items)
    }

    private data class Ride(
        val payerId: Long,
        val payeeId: Long,
        val boardingId: Long,
        val cost: RideCost,
        val manualFuelEconomy: Boolean,
    )
}
