package com.fullaccel.paircarserver.live.api

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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import kotlin.test.assertContains
import kotlin.test.assertFalse

@IntegrationTest
@AutoConfigureMockMvc
class LiveApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    private lateinit var jihoon: String // 운전자 토큰
    private lateinit var sua: String    // 동승자 토큰
    private var suaId = 0L
    private var tripId = 0L

    @BeforeEach
    fun setUp() {
        jihoon = mockMvc.createMemberToken(objectMapper, "지훈")
        mockMvc.put("/api/members/me/bank-account") {
            bearer(jihoon)
            json("""{"bankName": "○○은행", "accountNumber": "3333", "accountHolder": "지훈"}""")
        }
        val carId = mockMvc.post("/api/cars") {
            bearer(jihoon)
            json("""{"vehicleKey": "VK-1", "fuelType": "GASOLINE"}""")
        }.data(objectMapper)["carId"].asLong()
        val crewId = mockMvc.post("/api/crews") {
            bearer(jihoon)
            json("""{"name": "판교 출근 크루"}""")
        }.data(objectMapper)["crewId"].asLong()
        fun place(name: String) = mockMvc.post("/api/crews/$crewId/places") {
            bearer(jihoon)
            json("""{"name": "$name", "latitude": 37.4, "longitude": 127.1}""")
        }.data(objectMapper)["placeId"].asLong()
        val home = place("지훈 집")
        val gate = place("○○아파트 정문")
        val office = place("판교 본관")
        val code = mockMvc.post("/api/crews/$crewId/invites") { bearer(jihoon) }.data(objectMapper)["code"].asText()
        val joined = mockMvc.post("/api/invites/$code/members") { json("""{"nickname": "수아"}""") }.data(objectMapper)
        sua = joined["token"].asText()
        suaId = joined["crewMemberId"].asLong()
        tripId = mockMvc.post("/api/trips") {
            bearer(jihoon)
            json(
                """{"crewId": $crewId, "carId": $carId, "originPlaceId": $home, "destinationPlaceId": $office,
                    "startedAt": "2026-11-09T08:00:00+09:00", "startOdometerKm": 10000.0,
                    "riders": [{"crewMemberId": $suaId, "boardPlaceId": $gate, "alightPlaceId": $office}]}""",
            )
        }.data(objectMapper)["tripId"].asLong()
    }

    // 티켓을 받아 SSE를 구독하고, 비동기 응답을 돌려줌 (보낸 이벤트가 응답 본문에 쌓임)
    // SSE 본문은 UTF-8로 나감 (text/event-stream 규격). MockMvc 기본 문자셋이 ISO-8859-1이라 UTF-8로 읽음
    private fun subscribe(token: String): MvcResult {
        val ticket = mockMvc.post("/api/live/tickets") { bearer(token) }.data(objectMapper)["ticket"].asText()
        return mockMvc.get("/api/live/events?ticket=$ticket").andExpect {
            request { asyncStarted() }
        }.andReturn()
    }

    @Test
    fun `구독하면 connected 이벤트를 받는다`() {
        val stream = subscribe(sua)

        assertContains(stream.response.getContentAsString(Charsets.UTF_8), "event:connected")
    }

    @Test
    fun `운전자가 보낸 차량 위치를 동승자가 받는다`() {
        val stream = subscribe(sua)

        mockMvc.post("/api/trips/$tripId/live/location") {
            bearer(jihoon)
            json("""{"latitude": 37.401, "longitude": 127.108, "remainsByRider": [{"crewMemberId": $suaId, "remainSeconds": 180, "remainMeters": 1200}]}""")
        }.andExpect { status { isOk() } }

        val body = stream.response.getContentAsString(Charsets.UTF_8)
        assertContains(body, "event:vehicle.location")
        assertContains(body, "\"remainSeconds\":180")
    }

    @Test
    fun `동승자가 늦는다고 하면 운전자가 받고 동승자에게는 가지 않는다`() {
        val driverStream = subscribe(jihoon)
        val riderStream = subscribe(sua)

        mockMvc.post("/api/trips/$tripId/live/me/delay") {
            bearer(sua)
            json("""{"minutes": 5}""")
        }.andExpect { status { isOk() } }

        assertContains(driverStream.response.getContentAsString(Charsets.UTF_8), "event:rider.delayed")
        assertContains(driverStream.response.getContentAsString(Charsets.UTF_8), "\"nickname\":\"수아\"")
        assertFalse(riderStream.response.getContentAsString(Charsets.UTF_8).contains("rider.delayed"))
    }

    @Test
    fun `곧 도착 알림은 그 동승자에게 간다`() {
        val stream = subscribe(sua)

        mockMvc.post("/api/trips/$tripId/live/riders/$suaId/approaching") { bearer(jihoon) }.andExpect { status { isOk() } }

        assertContains(stream.response.getContentAsString(Charsets.UTF_8), "event:vehicle.approaching")
    }

    @Test
    fun `티켓은 한 번만 쓸 수 있다`() {
        val ticket = mockMvc.post("/api/live/tickets") { bearer(sua) }.data(objectMapper)["ticket"].asText()
        mockMvc.get("/api/live/events?ticket=$ticket").andExpect { request { asyncStarted() } }

        mockMvc.get("/api/live/events?ticket=$ticket").andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("LIVE_TICKET_INVALID") }
        }
    }

    @Test
    fun `운전자가 아니면 차량 위치를 보낼 수 없고, 동승자가 아니면 늦는다고 할 수 없다`() {
        val outsider = mockMvc.createMemberToken(objectMapper, "남")

        mockMvc.post("/api/trips/$tripId/live/location") {
            bearer(sua)
            json("""{"latitude": 37.4, "longitude": 127.1}""")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("TRIP_ACCESS_DENIED") }
        }
        mockMvc.post("/api/trips/$tripId/live/me/delay") {
            bearer(outsider)
            json("""{"minutes": 5}""")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `동승자는 자기 탑승을 불참 처리할 수 있다`() {
        mockMvc.delete("/api/trips/$tripId/boardings/$suaId") { bearer(sua) }.andExpect {
            status { isOk() }
            jsonPath("$.data.boardings.length()") { value(0) }
        }
    }
}
