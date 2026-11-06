package com.fullaccel.paircarserver.car.application

import com.fullaccel.paircarserver.car.application.dto.CarResult
import com.fullaccel.paircarserver.car.application.dto.RegisterCarCommand
import com.fullaccel.paircarserver.car.domain.Car
import com.fullaccel.paircarserver.car.domain.CarRepository
import com.fullaccel.paircarserver.car.domain.MemberCar
import com.fullaccel.paircarserver.car.domain.MemberCarRepository
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CarServiceImpl(
    private val carRepository: CarRepository,
    private val memberCarRepository: MemberCarRepository,
    private val memberService: MemberService,
) : CarService {

    @Transactional
    override fun registerCar(memberId: Long, command: RegisterCarCommand): CarResult {
        val member = memberService.getMemberEntity(memberId)
        val car = carRepository.findByVehicleKey(command.vehicleKey)
            ?: carRepository.save(
                Car(
                    vehicleKey = command.vehicleKey,
                    fuelType = command.fuelType,
                    manualFuelEconomy = command.manualFuelEconomy,
                ),
            )
        if (!memberCarRepository.existsByMemberIdAndCarId(memberId, car.id)) {
            memberCarRepository.save(MemberCar(member = member, car = car))
        }
        return CarResult.from(car)
    }

    override fun getCar(carId: Long): CarResult = CarResult.from(getCarEntity(carId))

    override fun getCarEntity(carId: Long): Car =
        carRepository.findByIdOrNull(carId)
            ?: throw BusinessException(ErrorCode.CAR_NOT_FOUND)

    override fun getCarsOfMember(memberId: Long): List<CarResult> {
        memberService.getMemberEntity(memberId)
        return memberCarRepository.findAllByMemberId(memberId).map { CarResult.from(it.car) }
    }

    override fun validateMemberCar(memberId: Long, carId: Long) {
        if (!memberCarRepository.existsByMemberIdAndCarId(memberId, carId)) {
            throw BusinessException(ErrorCode.CAR_NOT_REGISTERED_TO_MEMBER)
        }
    }
}
