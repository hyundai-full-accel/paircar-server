package com.fullaccel.paircarserver.car.application

import com.fullaccel.paircarserver.car.application.dto.CarResult
import com.fullaccel.paircarserver.car.application.dto.RegisterCarCommand
import com.fullaccel.paircarserver.car.application.dto.UpdateCarCommand
import com.fullaccel.paircarserver.car.domain.Car

interface CarService {

    // vehicleKey로 이미 등록된 차량이면 멤버만 연결하고, 없으면 차량을 만든 뒤 연결
    fun registerCar(memberId: Long, command: RegisterCarCommand): CarResult

    // 연료 종류, 입력 연비, 직접 입력 유가 수정. 그 차를 등록한 멤버만 (CAR_NOT_REGISTERED_TO_MEMBER)
    // 이미 발행한 영수증은 발행 때 값을 그대로 둠
    fun updateCar(memberId: Long, carId: Long, command: UpdateCarCommand): CarResult

    fun getCar(carId: Long): CarResult

    // 다른 패키지 서비스가 연관관계를 맺을 때 쓰는 엔티티 조회. 트랜잭션 안에서만 사용
    fun getCarEntity(carId: Long): Car

    fun getCarsOfMember(memberId: Long): List<CarResult>

    // 차를 한 대라도 등록했는지. 운전할 수 있는 멤버로 보고 크루 설정 권한에 씀 (동승자 웹은 차를 등록하지 않음)
    fun hasCar(memberId: Long): Boolean

    // 운행 시작 전 검증: 멤버가 이 차를 등록하지 않았으면 CAR_NOT_REGISTERED_TO_MEMBER
    fun validateMemberCar(memberId: Long, carId: Long)
}
