package com.fullaccel.paircarserver.settlement.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.support.bearer
import com.fullaccel.paircarserver.support.createMemberToken
import com.fullaccel.paircarserver.support.data
import com.fullaccel.paircarserver.support.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

// 기획서 시나리오를 API만으로 재현: 크루 만들기 → 초대 → 우회 거리 확인 → 18일 출근 → 월말 영수증
@IntegrationTest
@AutoConfigureMockMvc
class SettlementApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    private lateinit var jihoon: String
    private lateinit var sua: String
    private var suaId = 0L
    private var driverId = 0L
    private var crewId = 0L
    private var carId = 0L
    private var home = 0L
    private var gate = 0L
    private var office = 0L

    @BeforeEach
    fun setUp() {
        jihoon = mockMvc.createMemberToken(objectMapper, "지훈")
        mockMvc.put("/api/members/me/bank-account") {
            bearer(jihoon)
            json("""{"bankName": "○○은행", "accountNumber": "3333-01-1234567", "accountHolder": "지훈"}""")
        }
        carId = mockMvc.post("/api/cars") {
            bearer(jihoon)
            json("""{"vehicleKey": "VK-1", "fuelType": "GASOLINE"}""")
        }.data(objectMapper)["carId"].asLong()
        mockMvc.put("/api/cars/$carId") {
            bearer(jihoon)
            json("""{"fuelType": "GASOLINE", "manualFuelPriceWon": 1684}""")
        }
        crewId = mockMvc.post("/api/crews") {
            bearer(jihoon)
            json("""{"name": "판교 출근 크루"}""")
        }.data(objectMapper)["crewId"].asLong()
        driverId = mockMvc.get("/api/crews/$crewId/members") { bearer(jihoon) }.data(objectMapper)[0]["crewMemberId"].asLong()
        home = place("지훈 집")
        gate = place("○○아파트 정문")
        office = place("판교 본관")

        val code = mockMvc.post("/api/crews/$crewId/invites") { bearer(jihoon) }.data(objectMapper)["code"].asText()
        val joined = mockMvc.post("/api/invites/$code/members") { json("""{"nickname": "수아"}""") }.data(objectMapper)
        sua = joined["token"].asText()
        suaId = joined["crewMemberId"].asLong()

        val detourId = mockMvc.put("/api/crews/$crewId/detours") {
            bearer(jihoon)
            json("""{"passengerId": $suaId, "driverId": $driverId, "placeId": $gate, "detourKm": 3.0}""")
        }.data(objectMapper)["detourId"].asLong()
        mockMvc.post("/api/crews/$crewId/detours/$detourId/confirm") { bearer(jihoon) }
        mockMvc.post("/api/crews/$crewId/detours/$detourId/confirm") { bearer(sua) }
    }

    private fun place(name: String): Long =
        mockMvc.post("/api/crews/$crewId/places") {
            bearer(jihoon)
            json("""{"name": "$name", "latitude": 37.4, "longitude": 127.1}""")
        }.data(objectMapper)["placeId"].asLong()

    // 집 → (혼자 5km) → 정문 수아 탑승 → (함께 12km) → 본관
    private fun commute(day: Int) {
        val date = "2026-11-%02d".format(day)
        val start = 10000.0 + (day - 1) * 17
        val tripId = mockMvc.post("/api/trips") {
            bearer(jihoon)
            json(
                """{"crewId": $crewId, "carId": $carId, "originPlaceId": $home, "destinationPlaceId": $office,
                    "startedAt": "${date}T08:00:00+09:00", "startOdometerKm": $start,
                    "riders": [{"crewMemberId": $suaId, "boardPlaceId": $gate, "alightPlaceId": $office}]}""",
            )
        }.data(objectMapper)["tripId"].asLong()
        mockMvc.post("/api/trips/$tripId/boardings/$suaId/board") {
            bearer(jihoon)
            json("""{"occurredAt": "${date}T08:10:00+09:00", "odometerKm": ${start + 5}}""")
        }
        mockMvc.post("/api/trips/$tripId/segments") {
            bearer(jihoon)
            json(
                """{"segments": [
                    {"seq": 1, "startOdometerKm": $start, "endOdometerKm": ${start + 5}, "fuelEconomy": 13.20, "fuelEconomySource": "MEASURED", "riderCrewMemberIds": []},
                    {"seq": 2, "startOdometerKm": ${start + 5}, "endOdometerKm": ${start + 17}, "fuelEconomy": 13.20, "fuelEconomySource": "MEASURED", "riderCrewMemberIds": [$suaId]}
                ]}""",
            )
        }
        mockMvc.post("/api/trips/$tripId/end") {
            bearer(jihoon)
            json("""{"endedAt": "${date}T08:30:00+09:00", "endOdometerKm": ${start + 17}}""")
        }
    }

    @Test
    fun `18일 출근 뒤 월말 영수증은 20,700원이고 동승자가 조회할 수 있다`() {
        (1..18).forEach { commute(it) }

        val receiptId = mockMvc.post("/api/crews/$crewId/receipts?month=2026-11") { bearer(jihoon) }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(1) }
            jsonPath("$.data[0].amountWon") { value(20700) }
        }.data(objectMapper)[0]["receiptId"].asLong()

        mockMvc.get("/api/receipts/$receiptId") { bearer(sua) }.andExpect {
            status { isOk() }
            jsonPath("$.data.boardingDays") { value(18) }
            jsonPath("$.data.sharedDistanceKm") { value(216.0) }
            jsonPath("$.data.detourKm") { value(54.0) }
            jsonPath("$.data.ownDistanceKm") { value(162.0) }
            jsonPath("$.data.fuelEconomy") { value(13.2) }
            jsonPath("$.data.fuelPriceWon") { value(1684) }
            jsonPath("$.data.amountWon") { value(20700) }
            jsonPath("$.data.month") { value("2026-11") }
            jsonPath("$.data.payeeAccountNumber") { value("3333-01-1234567") }
            jsonPath("$.data.items.length()") { value(18) }
        }
        mockMvc.get("/api/crews/$crewId/receipts?month=2026-11") { bearer(sua) }.andExpect {
            jsonPath("$.data.length()") { value(1) }
        }
    }

    @Test
    fun `영수증 당사자가 아니면 403 RECEIPT_ACCESS_DENIED이고 목록에도 안 나온다`() {
        commute(2)
        val receiptId = mockMvc.post("/api/crews/$crewId/receipts?month=2026-11") { bearer(jihoon) }
            .data(objectMapper)[0]["receiptId"].asLong()
        val code = mockMvc.post("/api/crews/$crewId/invites") { bearer(jihoon) }.data(objectMapper)["code"].asText()
        val minjun = mockMvc.post("/api/invites/$code/members") { json("""{"nickname": "민준"}""") }.data(objectMapper)["token"].asText()

        mockMvc.get("/api/receipts/$receiptId") { bearer(minjun) }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("RECEIPT_ACCESS_DENIED") }
        }
        mockMvc.get("/api/crews/$crewId/receipts?month=2026-11") { bearer(minjun) }.andExpect {
            jsonPath("$.data.length()") { value(0) }
        }
    }

    @Test
    fun `정산 월 형식이 틀리면 INVALID_INPUT`() {
        mockMvc.post("/api/crews/$crewId/receipts?month=2026-13") { bearer(jihoon) }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
            jsonPath("$.error.fieldErrors[0].field") { value("month") }
        }
    }
}
