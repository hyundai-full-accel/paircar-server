package com.fullaccel.paircarserver.crew.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.crew.application.event.AttendanceChangedEvent
import com.fullaccel.paircarserver.support.bearer
import com.fullaccel.paircarserver.support.createMemberToken
import com.fullaccel.paircarserver.support.data
import com.fullaccel.paircarserver.support.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import kotlin.test.assertEquals

// 크루 일정, 평소 탈 곳, 출석. 테스트의 오늘은 2027-01-15 (TestClockConfiguration)
@IntegrationTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class CrewScheduleApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @Autowired
    private lateinit var events: ApplicationEvents

    private lateinit var jihoon: String // 운전자 (차 등록)
    private lateinit var sua: String    // 동승자 (차 없음)
    private var jihoonMemberId = 0L
    private var jihoonId = 0L
    private var suaId = 0L
    private var crewId = 0L
    private var home = 0L
    private var gate = 0L
    private var backGate = 0L
    private var office = 0L

    @BeforeEach
    fun setUp() {
        jihoon = mockMvc.createMemberToken(objectMapper, "지훈")
        mockMvc.post("/api/cars") {
            bearer(jihoon)
            json("""{"vehicleKey": "VK-1", "fuelType": "GASOLINE"}""")
        }
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
        val driver = mockMvc.get("/api/crews/$crewId/members") { bearer(jihoon) }.data(objectMapper)[0]
        jihoonId = driver["crewMemberId"].asLong()
        jihoonMemberId = driver["memberId"].asLong()
    }

    private fun place(name: String): Long =
        mockMvc.post("/api/crews/$crewId/places") {
            bearer(jihoon)
            json("""{"name": "$name", "latitude": 37.4, "longitude": 127.1}""")
        }.data(objectMapper)["placeId"].asLong()

    private fun crewSettings(mode: String = "FAIR", originPlaceId: Long = home) =
        """
        {"name": "판교 출근 크루", "departureTime": "08:00", "departureDays": ["FRIDAY", "MONDAY"],
         "originPlaceId": $originPlaceId, "destinationPlaceId": $office, "settlementMode": "$mode", "includeDriver": true}
        """

    @Test
    fun `차를 등록한 멤버가 크루 일정을 정하면 크루 조회에 나온다`() {
        mockMvc.put("/api/crews/$crewId") {
            bearer(jihoon)
            json(crewSettings())
        }.andExpect {
            status { isOk() }
        }

        mockMvc.get("/api/crews/$crewId") { bearer(sua) }.andExpect {
            jsonPath("$.data.departureTime") { value("08:00") }
            jsonPath("$.data.departureDays[0]") { value("MONDAY") } // 요일 순서로 정렬
            jsonPath("$.data.departureDays[1]") { value("FRIDAY") }
            jsonPath("$.data.originPlace.placeId") { value(home) }
            jsonPath("$.data.destinationPlace.name") { value("판교 본관") }
        }
    }

    @Test
    fun `차가 없는 동승자는 크루 설정을 바꿀 수 없다`() {
        mockMvc.put("/api/crews/$crewId") {
            bearer(sua)
            json(crewSettings(mode = "EQUAL"))
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("CREW_DRIVER_ONLY") }
        }
    }

    @Test
    fun `월 정액, 다른 크루 장소는 크루 설정에 쓸 수 없다`() {
        mockMvc.put("/api/crews/$crewId") {
            bearer(jihoon)
            json(crewSettings(mode = "FIXED_MONTHLY"))
        }.andExpect {
            jsonPath("$.error.code") { value("SETTLEMENT_MODE_NOT_SUPPORTED") }
        }

        val otherCrewId = mockMvc.post("/api/crews") {
            bearer(jihoon)
            json("""{"name": "다른 크루"}""")
        }.data(objectMapper)["crewId"].asLong()
        val otherPlace = mockMvc.post("/api/crews/$otherCrewId/places") {
            bearer(jihoon)
            json("""{"name": "다른 곳", "latitude": 37.4, "longitude": 127.1}""")
        }.data(objectMapper)["placeId"].asLong()
        mockMvc.put("/api/crews/$crewId") {
            bearer(jihoon)
            json(crewSettings(originPlaceId = otherPlace))
        }.andExpect {
            jsonPath("$.error.code") { value("CREW_MISMATCH") }
        }
    }

    @Test
    fun `동승자는 자기 평소 탈 곳을 정하고, 남의 것은 바꿀 수 없다`() {
        mockMvc.put("/api/crews/$crewId/members/$suaId/places") {
            bearer(sua)
            json("""{"boardPlaceId": $gate, "alightPlaceId": $office}""")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.boardPlace.placeId") { value(gate) }
        }
        mockMvc.get("/api/crews/$crewId/members") { bearer(jihoon) }.andExpect {
            jsonPath("$.data[1].alightPlace.placeId") { value(office) }
        }

        mockMvc.put("/api/crews/$crewId/members/$jihoonId/places") {
            bearer(sua)
            json("""{"boardPlaceId": $gate}""")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("CREW_MEMBER_SELF_OR_DRIVER_ONLY") }
        }
    }

    @Test
    fun `출석은 기본이 탐이고, 안 탄다고 하면 다른 크루 멤버에게 알린다`() {
        mockMvc.put("/api/crews/$crewId/members/$suaId/places") {
            bearer(sua)
            json("""{"boardPlaceId": $gate, "alightPlaceId": $office}""")
        }
        mockMvc.get("/api/crews/$crewId/attendance/2027-01-18") { bearer(jihoon) }.andExpect {
            jsonPath("$.data.length()") { value(2) }
            jsonPath("$.data[1].attending") { value(true) }
            jsonPath("$.data[1].boardPlace.placeId") { value(gate) }
            jsonPath("$.data[1].changed") { value(false) }
        }

        mockMvc.put("/api/crews/$crewId/members/$suaId/attendance/2027-01-18") {
            bearer(sua)
            json("""{"attending": false}""")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.attending") { value(false) }
        }
        mockMvc.get("/api/crews/$crewId/attendance/2027-01-18") { bearer(jihoon) }.andExpect {
            jsonPath("$.data[1].attending") { value(false) }
            jsonPath("$.data[1].changed") { value(true) }
        }

        val event = events.stream(AttendanceChangedEvent::class.java).toList().single()
        assertEquals(suaId, event.crewMemberId)
        assertEquals(false, event.attending)
        assertEquals(listOf(jihoonMemberId), event.recipientMemberIds)
    }

    @Test
    fun `그날만 다른 탈 곳을 정할 수 있고, 같은 값을 다시 보내면 알리지 않는다`() {
        repeat(2) {
            mockMvc.put("/api/crews/$crewId/members/$suaId/attendance/2027-01-15") {
                bearer(sua)
                json("""{"attending": true, "boardPlaceId": $backGate}""")
            }.andExpect {
                jsonPath("$.data.boardPlace.placeId") { value(backGate) }
            }
        }

        assertEquals(1, events.stream(AttendanceChangedEvent::class.java).count())
    }

    @Test
    fun `지난 날짜의 출석은 바꿀 수 없다`() {
        mockMvc.put("/api/crews/$crewId/members/$suaId/attendance/2027-01-14") {
            bearer(sua)
            json("""{"attending": false}""")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("ATTENDANCE_DATE_PASSED") }
        }
    }

    @Test
    fun `도착 시각, 평소 픽업 시각을 정하면 크루와 크루원 조회에 나온다`() {
        mockMvc.put("/api/crews/$crewId") {
            bearer(jihoon)
            json(
                """{"name": "판교 출근 크루", "departureTime": "07:10", "arrivalTime": "08:32", "departureDays": ["MONDAY"],
                    "settlementMode": "FAIR", "includeDriver": true}""",
            )
        }
        mockMvc.put("/api/crews/$crewId/members/$suaId/places") {
            bearer(sua)
            json("""{"boardPlaceId": $gate, "alightPlaceId": $office, "pickupTime": "07:32"}""")
        }

        mockMvc.get("/api/crews/$crewId") { bearer(sua) }.andExpect {
            jsonPath("$.data.arrivalTime") { value("08:32") }
        }
        mockMvc.get("/api/crews/$crewId/members") { bearer(sua) }.andExpect {
            jsonPath("$.data[1].pickupTime") { value("07:32") }
        }
    }

    @Test
    fun `출발 전에 그날 지각을 알리면 출석에 남고 다른 크루 멤버에게 알린다`() {
        mockMvc.put("/api/crews/$crewId/members/$suaId/attendance/2027-01-18") {
            bearer(sua)
            json("""{"attending": true, "delayMinutes": 10}""")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.delayMinutes") { value(10) }
        }

        val event = events.stream(AttendanceChangedEvent::class.java).toList().single()
        assertEquals(10, event.delayMinutes)

        // 안 타는 날로 바꾸면 지각은 지움
        mockMvc.put("/api/crews/$crewId/members/$suaId/attendance/2027-01-18") {
            bearer(sua)
            json("""{"attending": false, "delayMinutes": 10}""")
        }.andExpect {
            jsonPath("$.data.delayMinutes") { doesNotExist() }
        }
    }

    @Test
    fun `지각은 1분에서 120분까지`() {
        mockMvc.put("/api/crews/$crewId/members/$suaId/attendance/2027-01-18") {
            bearer(sua)
            json("""{"attending": true, "delayMinutes": 0}""")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
        }
    }
}
