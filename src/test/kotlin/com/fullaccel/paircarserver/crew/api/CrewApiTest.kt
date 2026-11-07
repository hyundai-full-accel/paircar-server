package com.fullaccel.paircarserver.crew.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.crew.domain.CrewInvite
import com.fullaccel.paircarserver.crew.domain.CrewInviteRepository
import com.fullaccel.paircarserver.crew.domain.CrewRepository
import com.fullaccel.paircarserver.support.bearer
import com.fullaccel.paircarserver.support.createMemberToken
import com.fullaccel.paircarserver.support.data
import com.fullaccel.paircarserver.support.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@IntegrationTest
@AutoConfigureMockMvc
class CrewApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val crewRepository: CrewRepository,
    @Autowired private val crewInviteRepository: CrewInviteRepository,
) {

    private lateinit var jihoon: String // 운전자 토큰
    private var crewId = 0L

    @BeforeEach
    fun setUp() {
        jihoon = mockMvc.createMemberToken(objectMapper, "지훈")
        crewId = mockMvc.post("/api/crews") {
            bearer(jihoon)
            json("""{"name": "판교 출근 크루"}""")
        }.andExpect { status { isCreated() } }.data(objectMapper)["crewId"].asLong()
    }

    private fun invite(): String =
        mockMvc.post("/api/crews/$crewId/invites") { bearer(jihoon) }
            .andExpect { status { isCreated() } }
            .data(objectMapper)["code"].asText()

    // 초대 링크로 새 동승자가 참여하고 토큰과 크루 멤버 ID를 돌려줌
    private fun joinAsNewMember(nickname: String): Pair<String, Long> {
        val data = mockMvc.post("/api/invites/${invite()}/members") { json("""{"nickname": "$nickname"}""") }
            .andExpect { status { isCreated() } }
            .data(objectMapper)
        return data["token"].asText() to data["crewMemberId"].asLong()
    }

    private fun registerPlace(name: String): Long =
        mockMvc.post("/api/crews/$crewId/places") {
            bearer(jihoon)
            json("""{"name": "$name", "latitude": 37.401234, "longitude": 127.108765}""")
        }.andExpect { status { isCreated() } }.data(objectMapper)["placeId"].asLong()

    private fun crewMemberIdOf(token: String): Long {
        val members = mockMvc.get("/api/crews/$crewId/members") { bearer(token) }.data(objectMapper)
        val me = mockMvc.get("/api/members/me") { bearer(token) }.data(objectMapper)["memberId"].asLong()
        return members.first { it["memberId"].asLong() == me }["crewMemberId"].asLong()
    }

    @Test
    fun `크루를 만들면 내 크루 목록과 멤버 목록에 나온다`() {
        mockMvc.get("/api/crews/me") { bearer(jihoon) }.andExpect {
            jsonPath("$.data[0].crewId") { value(crewId) }
            jsonPath("$.data[0].settlementMode") { value("FAIR") }
        }
        mockMvc.get("/api/crews/$crewId/members") { bearer(jihoon) }.andExpect {
            jsonPath("$.data[0].nickname") { value("지훈") }
        }
    }

    @Test
    fun `크루 멤버가 아니면 403 CREW_ACCESS_DENIED`() {
        val outsider = mockMvc.createMemberToken(objectMapper, "남")

        mockMvc.get("/api/crews/$crewId/members") { bearer(outsider) }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("CREW_ACCESS_DENIED") }
        }
    }

    @Test
    fun `초대 링크로 처음 온 동승자가 참여하면 토큰으로 크루를 조회할 수 있다`() {
        val code = invite()
        mockMvc.get("/api/invites/$code").andExpect {
            status { isOk() }
            jsonPath("$.data.crewName") { value("판교 출근 크루") }
        }

        val (suaToken, _) = joinAsNewMember("수아")

        mockMvc.get("/api/crews/$crewId/members") { bearer(suaToken) }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(2) }
        }
    }

    @Test
    fun `초대 미리보기는 토큰 없이 크루 일정, 인원, 운전자 이름을 보여 준다`() {
        mockMvc.post("/api/cars") {
            bearer(jihoon)
            json("""{"vehicleKey": "VK-1", "fuelType": "GASOLINE"}""")
        }
        val home = registerPlace("지훈 집")
        val office = registerPlace("판교 본관")
        mockMvc.put("/api/crews/$crewId") {
            bearer(jihoon)
            json(
                """{"name": "판교 출근 크루", "departureTime": "07:10", "departureDays": ["MONDAY", "FRIDAY"],
                    "originPlaceId": $home, "destinationPlaceId": $office, "settlementMode": "FAIR", "includeDriver": true}""",
            )
        }
        joinAsNewMember("수아")

        mockMvc.get("/api/invites/${invite()}").andExpect {
            status { isOk() }
            jsonPath("$.data.departureTime") { value("07:10") }
            jsonPath("$.data.departureDays[1]") { value("FRIDAY") }
            jsonPath("$.data.originPlace.name") { value("지훈 집") }
            jsonPath("$.data.destinationPlace.name") { value("판교 본관") }
            jsonPath("$.data.memberCount") { value(2) }
            jsonPath("$.data.driverNicknames.length()") { value(1) }
            jsonPath("$.data.driverNicknames[0]") { value("지훈") }
            jsonPath("$.data.settlementMode") { value("FAIR") }
            jsonPath("$.data.places.length()") { value(2) }
        }
    }

    @Test
    fun `크루원 목록은 차를 등록한 멤버를 운전자로 표시한다`() {
        mockMvc.post("/api/cars") {
            bearer(jihoon)
            json("""{"vehicleKey": "VK-1", "fuelType": "GASOLINE"}""")
        }
        joinAsNewMember("수아")

        mockMvc.get("/api/crews/$crewId/members") { bearer(jihoon) }.andExpect {
            jsonPath("$.data[0].nickname") { value("지훈") }
            jsonPath("$.data[0].driver") { value(true) }
            jsonPath("$.data[1].driver") { value(false) }
        }
    }

    @Test
    fun `초대 코드는 헷갈리는 글자 없는 8자이고 링크는 설정한 주소로 만든다`() {
        val data = mockMvc.post("/api/crews/$crewId/invites") { bearer(jihoon) }.data(objectMapper)
        val code = data["code"].asText()

        assertTrue(Regex("[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{8}").matches(code), code)
        assertEquals("https://paircar.test/join/$code", data["inviteUrl"].asText())
    }

    @Test
    fun `사람이 입력한 코드는 대소문자와 하이픈을 무시한다`() {
        val code = invite()
        val typed = code.lowercase().chunked(4).joinToString("-") // K7M2QX9P → k7m2-qx9p

        mockMvc.get("/api/invites/$typed").andExpect {
            status { isOk() }
            jsonPath("$.data.code") { value(code) }
        }
    }

    @Test
    fun `토큰이 있는 멤버는 초대 코드로 바로 참여하고 두 번 참여하면 409`() {
        val minjun = mockMvc.createMemberToken(objectMapper, "민준")
        val code = invite()

        mockMvc.post("/api/invites/$code/join") { bearer(minjun) }.andExpect { status { isOk() } }
        mockMvc.post("/api/invites/$code/join") { bearer(minjun) }.andExpect {
            status { isConflict() }
            jsonPath("$.error.code") { value("CREW_MEMBER_ALREADY_EXISTS") }
        }
    }

    @Test
    fun `없는 초대 코드는 404, 만료된 초대 코드는 410`() {
        mockMvc.get("/api/invites/nope").andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("INVITE_NOT_FOUND") }
        }

        crewInviteRepository.save(
            CrewInvite(crew = crewRepository.findByIdOrNull(crewId)!!, code = "EXPRD234", expiresAt = Instant.now().minusSeconds(1)),
        )
        mockMvc.post("/api/invites/EXPRD234/members") { json("""{"nickname": "수아"}""") }.andExpect {
            status { isGone() }
            jsonPath("$.error.code") { value("INVITE_EXPIRED") }
        }
    }

    @Test
    fun `장소를 등록하고 이름이 같으면 409, 위도가 범위를 벗어나면 400`() {
        registerPlace("○○아파트 정문")

        mockMvc.get("/api/crews/$crewId/places") { bearer(jihoon) }.andExpect {
            jsonPath("$.data[0].name") { value("○○아파트 정문") }
        }
        mockMvc.post("/api/crews/$crewId/places") {
            bearer(jihoon)
            json("""{"name": "○○아파트 정문", "latitude": 37.4, "longitude": 127.1}""")
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error.code") { value("CREW_PLACE_NAME_DUPLICATED") }
        }
        mockMvc.post("/api/crews/$crewId/places") {
            bearer(jihoon)
            json("""{"name": "북극", "latitude": 91, "longitude": 127.1}""")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.fieldErrors[0].field") { value("latitude") }
        }
    }

    @Test
    fun `우회 거리를 등록하고 운전자와 동승자가 각자 확인하면 confirmed`() {
        val (suaToken, suaId) = joinAsNewMember("수아")
        val driverId = crewMemberIdOf(jihoon)
        val gate = registerPlace("○○아파트 정문")

        val detourId = mockMvc.put("/api/crews/$crewId/detours") {
            bearer(jihoon)
            json("""{"passengerId": $suaId, "driverId": $driverId, "placeId": $gate, "detourKm": 3.0}""")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.confirmed") { value(false) }
        }.data(objectMapper)["detourId"].asLong()

        mockMvc.post("/api/crews/$crewId/detours/$detourId/confirm") { bearer(jihoon) }
        mockMvc.post("/api/crews/$crewId/detours/$detourId/confirm") { bearer(suaToken) }.andExpect {
            status { isOk() }
            jsonPath("$.data.confirmed") { value(true) }
        }
    }

    @Test
    fun `우회 거리 당사자가 아닌 크루 멤버가 확인하면 403`() {
        val (_, suaId) = joinAsNewMember("수아")
        val (minjunToken, _) = joinAsNewMember("민준")
        val gate = registerPlace("○○아파트 정문")
        val detourId = mockMvc.put("/api/crews/$crewId/detours") {
            bearer(jihoon)
            json("""{"passengerId": $suaId, "driverId": ${crewMemberIdOf(jihoon)}, "placeId": $gate, "detourKm": 3.0}""")
        }.data(objectMapper)["detourId"].asLong()

        mockMvc.post("/api/crews/$crewId/detours/$detourId/confirm") { bearer(minjunToken) }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("DETOUR_CONFIRMER_NOT_PARTY") }
        }
    }
}
