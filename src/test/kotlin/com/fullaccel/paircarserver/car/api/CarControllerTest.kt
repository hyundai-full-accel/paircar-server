package com.fullaccel.paircarserver.car.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.support.bearer
import com.fullaccel.paircarserver.support.createMemberToken
import com.fullaccel.paircarserver.support.data
import com.fullaccel.paircarserver.support.json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@IntegrationTest
@AutoConfigureMockMvc
class CarControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @Test
    fun `차량을 등록하면 내 차량 목록에 나온다`() {
        val token = mockMvc.createMemberToken(objectMapper, "지훈")

        val carId = mockMvc.post("/api/cars") {
            bearer(token)
            json("""{"vehicleKey": "VK-1", "fuelType": "GASOLINE"}""")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.fuelType") { value("GASOLINE") }
        }.data(objectMapper)["carId"].asLong()

        mockMvc.get("/api/cars/me") { bearer(token) }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].carId") { value(carId) }
        }
        mockMvc.get("/api/cars/$carId") { bearer(token) }.andExpect {
            status { isOk() }
            jsonPath("$.data.vehicleKey") { value("VK-1") }
        }
    }

    @Test
    fun `입력 연비를 함께 등록한다`() {
        val token = mockMvc.createMemberToken(objectMapper, "지훈")

        mockMvc.post("/api/cars") {
            bearer(token)
            json("""{"vehicleKey": "VK-1", "fuelType": "EV", "manualFuelEconomy": 5.40}""")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.manualFuelEconomy") { value(5.4) }
        }
    }

    @Test
    fun `없는 연료 종류면 INVALID_JSON과 필드 이름`() {
        val token = mockMvc.createMemberToken(objectMapper, "지훈")

        mockMvc.post("/api/cars") {
            bearer(token)
            json("""{"vehicleKey": "VK-1", "fuelType": "WATER"}""")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_JSON") }
            jsonPath("$.error.fieldErrors[0].field") { value("fuelType") }
        }
    }

    @Test
    fun `입력 연비가 0 이하면 INVALID_INPUT`() {
        val token = mockMvc.createMemberToken(objectMapper, "지훈")

        mockMvc.post("/api/cars") {
            bearer(token)
            json("""{"vehicleKey": "VK-1", "fuelType": "GASOLINE", "manualFuelEconomy": 0}""")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
            jsonPath("$.error.fieldErrors[0].field") { value("manualFuelEconomy") }
        }
    }

    @Test
    fun `없는 차량이면 404 CAR_NOT_FOUND`() {
        val token = mockMvc.createMemberToken(objectMapper, "지훈")

        mockMvc.get("/api/cars/999999") { bearer(token) }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("CAR_NOT_FOUND") }
        }
    }

    @Test
    fun `토큰이 없으면 401`() {
        mockMvc.get("/api/cars/me").andExpect { status { isUnauthorized() } }
    }
}
