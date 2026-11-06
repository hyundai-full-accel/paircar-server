package com.fullaccel.paircarserver.live.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.live.domain.PushSubscriptionRepository
import com.fullaccel.paircarserver.support.bearer
import com.fullaccel.paircarserver.support.createMemberToken
import com.fullaccel.paircarserver.support.json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import kotlin.test.assertEquals
import kotlin.test.assertNull

@IntegrationTest
@AutoConfigureMockMvc
class PushApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val pushSubscriptionRepository: PushSubscriptionRepository,
) {

    private val endpoint = "https://push.example.com/send/abc"

    private fun subscribeBody(p256dh: String = "BPublicKey") =
        """{"endpoint": "$endpoint", "keys": {"p256dh": "$p256dh", "auth": "AuthSecret"}}"""

    @Test
    fun `VAPID 키가 없으면 Push가 꺼져 있다고 알려준다`() {
        mockMvc.get("/api/push/config").andExpect {
            status { isOk() }
            jsonPath("$.data.enabled") { value(false) }
            jsonPath("$.data.publicKey") { doesNotExist() }
        }
    }

    @Test
    fun `구독을 등록하고 같은 브라우저로 다른 멤버가 등록하면 그 멤버로 바뀐다`() {
        val sua = mockMvc.createMemberToken(objectMapper, "수아")
        val minjun = mockMvc.createMemberToken(objectMapper, "민준")

        mockMvc.put("/api/push/subscriptions") { bearer(sua); json(subscribeBody()) }.andExpect { status { isOk() } }
        val suaMemberId = pushSubscriptionRepository.findByEndpoint(endpoint)!!.memberId
        mockMvc.put("/api/push/subscriptions") { bearer(minjun); json(subscribeBody("BNewKey")) }.andExpect { status { isOk() } }

        val subscription = pushSubscriptionRepository.findByEndpoint(endpoint)!!
        assert(subscription.memberId != suaMemberId)
        assertEquals("BNewKey", subscription.p256dh)
        assertEquals(1, pushSubscriptionRepository.count())
    }

    @Test
    fun `다른 멤버의 구독은 해제할 수 없고 내 구독은 해제된다`() {
        val sua = mockMvc.createMemberToken(objectMapper, "수아")
        val minjun = mockMvc.createMemberToken(objectMapper, "민준")
        mockMvc.put("/api/push/subscriptions") { bearer(sua); json(subscribeBody()) }

        mockMvc.delete("/api/push/subscriptions") { bearer(minjun); json("""{"endpoint": "$endpoint"}""") }
        assertEquals(1, pushSubscriptionRepository.count())

        mockMvc.delete("/api/push/subscriptions") { bearer(sua); json("""{"endpoint": "$endpoint"}""") }
        assertNull(pushSubscriptionRepository.findByEndpoint(endpoint))
    }

    @Test
    fun `키가 빠지면 INVALID_INPUT`() {
        val sua = mockMvc.createMemberToken(objectMapper, "수아")

        mockMvc.put("/api/push/subscriptions") {
            bearer(sua)
            json("""{"endpoint": "$endpoint", "keys": {"p256dh": "", "auth": "AuthSecret"}}""")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.fieldErrors[0].field") { value("keys.p256dh") }
        }
    }
}
