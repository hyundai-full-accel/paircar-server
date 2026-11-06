package com.fullaccel.paircarserver.car.application

import com.fullaccel.paircarserver.car.application.dto.CarResult
import com.fullaccel.paircarserver.car.application.dto.RegisterCarCommand
import com.fullaccel.paircarserver.car.domain.Car

interface CarService {

    // vehicleKey로 이미 등록된 차량이면 멤버만 연결하고, 없으면 차량을 만든 뒤 연결
    fun registerCar(memberId: Long, command: RegisterCarCommand): CarResult

    fun getCar(carId: Long): CarResult

    // 다른 패키지 서비스가 연관관계를 맺을 때 쓰는 엔티티 조회. 트랜잭션 안에서만 사용
    fun getCarEntity(carId: Long): Car

    fun getCarsOfMember(memberId: Long): List<CarResult>

    // 운행 시작 전 검증: 멤버가 이 차를 등록하지 않았으면 CAR_NOT_REGISTERED_TO_MEMBER
    fun validateMemberCar(memberId: Long, carId: Long)
}
