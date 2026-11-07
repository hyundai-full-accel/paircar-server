package com.fullaccel.paircarserver.trip.api

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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@IntegrationTest
@AutoConfigureMockMvc
class TripApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    private lateinit var jihoon: String // 운전자 토큰
    private lateinit var sua: String    // 동승자 토큰
    private var suaId = 0L               // 수아 크루 멤버 ID
    private var crewId = 0L
    private var carId = 0L
    private var home = 0L
    private var gate = 0L
    private var backGate = 0L
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
        crewId = mockMvc.post("/api/crews") {
            bearer(jihoon)
            json("""{"name": "판교 출근 크루"}""")
        }.data(objectMapper)["crewId"].asLong()
        home = place("지훈 집")
        gate = place("○○아파트 정문")
        backGate = place("○○아파트 후문")
        office = place("판교 본관")

        val code = mockMvc.post("/api/crews/$crewId/invites") { bearer(jihoon) }.data(objectMapper)["code"].asText()
        val joined = mockMvc.post("/api/invites/$code/members") { json("""{"nickname": "수아"}""") }.data(objectMapper)
        sua = joined["token"].asText()
        suaId = joined["crewMemberId"].asLong()
    }

    private fun place(name: String): Long =
        mockMvc.post("/api/crews/$crewId/places") {
            bearer(jihoon)
            json("""{"name": "$name", "latitude": 37.4, "longitude": 127.1}""")
        }.data(objectMapper)["placeId"].asLong()

    private fun startTrip(): Long =
        mockMvc.post("/api/trips") {
            bearer(jihoon)
            json(
                """
                {"crewId": $crewId, "carId": $carId, "originPlaceId": $home, "destinationPlaceId": $office,
                 "startedAt": "2026-11-09T08:00:00+09:00", "startOdometerKm": 10000.0,
                 "riders": [{"crewMemberId": $suaId, "boardPlaceId": $gate, "alightPlaceId": $office}]}
                """,
            )
        }.andExpect { status { isCreated() } }.data(objectMapper)["tripId"].asLong()

    @Test
    fun `시동부터 도착까지 한 번의 출근`() {
        val tripId = startTrip()
        mockMvc.get("/api/trips/$tripId") { bearer(jihoon) }.andExpect {
            jsonPath("$.data.waypoints[0].placeId") { value(gate) }
            jsonPath("$.data.serviceDate") { value("2026-11-09") }
        }

        mockMvc.post("/api/trips/$tripId/boardings/$suaId/board") {
            bearer(jihoon)
            json("""{"occurredAt": "2026-11-09T08:10:00+09:00", "odometerKm": 10005.0}""")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.boardings[0].boardOdometerKm") { value(10005.0) }
        }

        mockMvc.post("/api/trips/$tripId/segments") {
            bearer(jihoon)
            json(
                """
                {"segments": [
                  {"seq": 1, "startOdometerKm": 10000.0, "endOdometerKm": 10005.0, "fuelEconomy": 13.20, "fuelEconomySource": "MEASURED", "riderCrewMemberIds": []},
                  {"seq": 2, "startOdometerKm": 10005.0, "endOdometerKm": 10017.0, "fuelEconomy": 13.20, "fuelEconomySource": "MEASURED", "riderCrewMemberIds": [$suaId]}
                ]}
                """,
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.segments.length()") { value(2) }
            jsonPath("$.data.segments[1].riderCrewMemberIds[0]") { value(suaId) }
        }

        mockMvc.post("/api/trips/$tripId/end") {
            bearer(jihoon)
            json("""{"endedAt": "2026-11-09T08:30:00+09:00", "endOdometerKm": 10017.0, "tollFareWon": 0}""")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.endOdometerKm") { value(10017.0) }
            jsonPath("$.data.boardings[0].alightOdometerKm") { value(10017.0) } // 하차 기록 없어 도착 값으로 하차
        }

        // 동승자도 크루 멤버라 운행을 조회할 수 있음
        mockMvc.get("/api/trips/$tripId") { bearer(sua) }.andExpect { status { isOk() } }
    }

    @Test
    fun `동승자는 진행 중인 운행을 조회하고, 끝나면 data가 없다`() {
        mockMvc.get("/api/crews/$crewId/trips/current") { bearer(sua) }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data") { doesNotExist() }
        }

        val tripId = startTrip()
        mockMvc.get("/api/crews/$crewId/trips/current") { bearer(sua) }.andExpect {
            jsonPath("$.data.tripId") { value(tripId) }
        }

        mockMvc.post("/api/trips/$tripId/end") {
            bearer(jihoon)
            json("""{"endedAt": "2026-11-09T08:30:00+09:00", "endOdometerKm": 10017.0, "tollFareWon": 0}""")
        }
        mockMvc.get("/api/crews/$crewId/trips/current") { bearer(sua) }.andExpect {
            jsonPath("$.data") { doesNotExist() }
        }
    }

    @Test
    fun `진행 중인 운행이 있는데 또 시작하면 409와 함께 그 운행 ID를 준다`() {
        val tripId = startTrip()

        mockMvc.post("/api/trips") {
            bearer(jihoon)
            json(
                """
                {"crewId": $crewId, "carId": $carId, "originPlaceId": $home, "destinationPlaceId": $office,
                 "startedAt": "2026-11-09T09:00:00+09:00", "startOdometerKm": 10020.0, "riders": []}
                """,
            )
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error.code") { value("TRIP_ALREADY_IN_PROGRESS") }
            jsonPath("$.data.tripId") { value(tripId) }
            jsonPath("$.data.crewId") { value(crewId) }
        }
    }

    @Test
    fun `탈 지점을 바꾸고 불참 처리한다`() {
        val tripId = startTrip()

        mockMvc.put("/api/trips/$tripId/boardings/$suaId/board-place") {
            bearer(jihoon)
            json("""{"placeId": $backGate}""")
        }.andExpect {
            jsonPath("$.data.waypoints[0].placeId") { value(backGate) }
        }

        mockMvc.delete("/api/trips/$tripId/boardings/$suaId") { bearer(jihoon) }.andExpect {
            status { isOk() }
            jsonPath("$.data.boardings.length()") { value(0) }
        }
    }

    @Test
    fun `운전자가 아닌 크루 멤버가 탑승을 기록하면 403 TRIP_ACCESS_DENIED`() {
        val tripId = startTrip()

        mockMvc.post("/api/trips/$tripId/boardings/$suaId/board") {
            bearer(sua)
            json("""{"occurredAt": "2026-11-09T08:10:00+09:00", "odometerKm": 10005.0}""")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("TRIP_ACCESS_DENIED") }
        }
    }

    @Test
    fun `크루 밖 멤버는 운행을 조회할 수 없다`() {
        val tripId = startTrip()
        val outsider = mockMvc.createMemberToken(objectMapper, "남")

        mockMvc.get("/api/trips/$tripId") { bearer(outsider) }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("CREW_ACCESS_DENIED") }
        }
    }

    @Test
    fun `출발 시각이 없으면 INVALID_JSON, 주행거리가 음수면 INVALID_INPUT`() {
        mockMvc.post("/api/trips") {
            bearer(jihoon)
            json("""{"crewId": $crewId, "carId": $carId, "originPlaceId": $home, "destinationPlaceId": $office, "startOdometerKm": 10000.0}""")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_JSON") }
            jsonPath("$.error.fieldErrors[0].field") { value("startedAt") }
        }

        mockMvc.post("/api/trips") {
            bearer(jihoon)
            json(
                """{"crewId": $crewId, "carId": $carId, "originPlaceId": $home, "destinationPlaceId": $office,
                    "startedAt": "2026-11-09T08:00:00+09:00", "startOdometerKm": -1}""",
            )
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
            jsonPath("$.error.fieldErrors[0].field") { value("startOdometerKm") }
        }
    }

    @Test
    fun `구간 목록이 비어 있으면 INVALID_INPUT`() {
        val tripId = startTrip()

        mockMvc.post("/api/trips/$tripId/segments") {
            bearer(jihoon)
            json("""{"segments": []}""")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.fieldErrors[0].field") { value("segments") }
        }
    }
}
