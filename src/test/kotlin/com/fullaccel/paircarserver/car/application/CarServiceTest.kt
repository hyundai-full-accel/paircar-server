package com.fullaccel.paircarserver.car.application

import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.car.application.dto.RegisterCarCommand
import com.fullaccel.paircarserver.car.domain.FuelType
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.MemberService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

@IntegrationTest
class CarServiceTest(
    @Autowired private val carService: CarService,
    @Autowired private val memberService: MemberService,
) {

    @Test
    fun `차량을 등록하면 멤버의 차량 목록에 나온다`() {
        val member = memberService.createMember("지훈")

        val car = carService.registerCar(member.memberId, RegisterCarCommand("VK-1", FuelType.GASOLINE))

        assertEquals(listOf(car.carId), carService.getCarsOfMember(member.memberId).map { it.carId })
    }

    @Test
    fun `같은 vehicleKey로 다른 멤버가 등록하면 같은 차량에 연결된다`() {
        val jihoon = memberService.createMember("지훈")
        val sua = memberService.createMember("수아")

        val first = carService.registerCar(jihoon.memberId, RegisterCarCommand("VK-1", FuelType.GASOLINE))
        val second = carService.registerCar(sua.memberId, RegisterCarCommand("VK-1", FuelType.GASOLINE))

        assertEquals(first.carId, second.carId)
        assertDoesNotThrow { carService.validateMemberCar(sua.memberId, first.carId) }
    }

    @Test
    fun `같은 멤버가 같은 차량을 다시 등록해도 중복 연결되지 않는다`() {
        val member = memberService.createMember("지훈")

        carService.registerCar(member.memberId, RegisterCarCommand("VK-1", FuelType.GASOLINE))
        carService.registerCar(member.memberId, RegisterCarCommand("VK-1", FuelType.GASOLINE))

        assertEquals(1, carService.getCarsOfMember(member.memberId).size)
    }

    @Test
    fun `등록하지 않은 차량이면 CAR_NOT_REGISTERED_TO_MEMBER`() {
        val owner = memberService.createMember("지훈")
        val other = memberService.createMember("수아")
        val car = carService.registerCar(owner.memberId, RegisterCarCommand("VK-1", FuelType.EV))

        val e = assertThrows<BusinessException> { carService.validateMemberCar(other.memberId, car.carId) }

        assertEquals(ErrorCode.CAR_NOT_REGISTERED_TO_MEMBER, e.errorCode)
    }

    @Test
    fun `없는 차량을 조회하면 CAR_NOT_FOUND`() {
        val e = assertThrows<BusinessException> { carService.getCar(999_999L) }

        assertEquals(ErrorCode.CAR_NOT_FOUND, e.errorCode)
    }

    @Test
    fun `없는 멤버가 차량을 등록하면 MEMBER_NOT_FOUND`() {
        val e = assertThrows<BusinessException> {
            carService.registerCar(999_999L, RegisterCarCommand("VK-1", FuelType.GASOLINE))
        }

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, e.errorCode)
    }
}
