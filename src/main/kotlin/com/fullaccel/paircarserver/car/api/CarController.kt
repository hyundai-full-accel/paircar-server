package com.fullaccel.paircarserver.car.api

import com.fullaccel.paircarserver.car.api.dto.RegisterCarRequest
import com.fullaccel.paircarserver.car.api.dto.UpdateCarRequest
import com.fullaccel.paircarserver.car.application.CarService
import com.fullaccel.paircarserver.car.application.dto.CarResult
import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "차량", description = "차량 등록, 수정, 조회")
@RestController
@RequestMapping("/api/cars")
class CarController(
    private val carService: CarService,
) {

    @Operation(
        summary = "차량 등록",
        description = "내 차량으로 등록합니다. 같은 vehicleKey가 이미 있으면 그 차량에 나를 연결하고, 연료 종류와 입력 연비는 기존 값을 유지합니다 (바꾸려면 차량 수정). " +
            "운행을 시작하려면 운전자가 그 차량을 등록해 둬야 합니다.",
    )
    @PostMapping
    fun registerCar(
        @LoginMember memberId: Long,
        @Valid @RequestBody request: RegisterCarRequest,
    ): ApiResponse<CarResult> =
        ApiResponse.ok(carService.registerCar(memberId, request.toCommand()))

    @Operation(
        summary = "차량 수정",
        description = "연료 종류, 입력 연비, 직접 입력 유가를 바꿉니다. 바꿀 수 있는 값 전체를 보내고, null은 그 값을 지웁니다. " +
            "그 차를 등록한 멤버만 바꿀 수 있습니다. 정산은 운행에 쓴 차량의 유가를 쓰고, 없으면 연료별 공공 월평균을 씁니다. " +
            "이미 발행한 영수증은 바뀌지 않습니다.",
    )
    @PutMapping("/{carId}")
    fun updateCar(
        @LoginMember memberId: Long,
        @PathVariable carId: Long,
        @Valid @RequestBody request: UpdateCarRequest,
    ): ApiResponse<CarResult> =
        ApiResponse.ok(carService.updateCar(memberId, carId, request.toCommand()))

    @Operation(summary = "내 차량 목록")
    @GetMapping("/me")
    fun getMyCars(@LoginMember memberId: Long): ApiResponse<List<CarResult>> =
        ApiResponse.ok(carService.getCarsOfMember(memberId))

    @Operation(summary = "차량 조회")
    @GetMapping("/{carId}")
    fun getCar(
        @LoginMember memberId: Long,
        @PathVariable carId: Long,
    ): ApiResponse<CarResult> =
        ApiResponse.ok(carService.getCar(carId))
}
