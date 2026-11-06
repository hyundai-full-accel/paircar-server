package com.fullaccel.paircarserver.live.push

import com.fasterxml.jackson.databind.ObjectMapper
import com.fullaccel.paircarserver.live.application.dto.PushMessage
import com.fullaccel.paircarserver.live.application.dto.PushUrgency
import com.fullaccel.paircarserver.live.domain.PushSubscription
import com.fullaccel.paircarserver.live.domain.PushSubscriptionRepository
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 실제 VAPID 서명, 메시지 암호화가 동작하는지 가짜 푸시 서비스로 확인
class WebPushSenderTest {

    private val requests = CopyOnWriteArrayList<Map<String, String>>()
    private var responseStatus = 201
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/push") { exchange ->
            requests += exchange.requestHeaders.mapValues { it.value.first() }.mapKeys { it.key.lowercase() }
            exchange.sendResponseHeaders(responseStatus, -1)
            exchange.close()
        }
        start()
    }
    private val endpoint = "http://127.0.0.1:${server.address.port}/push"
    private val repository = mock(PushSubscriptionRepository::class.java)

    @AfterEach
    fun tearDown() = server.stop(0)

    private fun base64Url(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun unsigned32(value: BigInteger) = value.toByteArray().takeLast(32).toByteArray().let { ByteArray(32 - it.size) + it }

    // P-256 키쌍을 (공개키: 비압축 점 65바이트, 비밀키: 32바이트) base64url로
    private fun newKeyPair(): Pair<String, String> {
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val point = (pair.public as ECPublicKey).w
        val publicKey = byteArrayOf(0x04) + unsigned32(point.affineX) + unsigned32(point.affineY)
        return base64Url(publicKey) to base64Url(unsigned32((pair.private as ECPrivateKey).s))
    }

    private fun sender(): WebPushSender {
        val (vapidPublic, vapidPrivate) = newKeyPair()
        val (browserPublic, _) = newKeyPair()
        `when`(repository.findAllByMemberIdIn(anyCollection())).thenReturn(
            listOf(PushSubscription(memberId = 1, endpoint = endpoint, p256dh = browserPublic, auth = base64Url(ByteArray(16) { it.toByte() }))),
        )
        return WebPushSender(WebPushProperties(vapidPublic, vapidPrivate, "mailto:test@paircar.test"), repository, ObjectMapper())
    }

    private val message = PushMessage("vehicle.approaching", "차가 곧 도착해요", "지금 나가면 딱 맞아요", "/trips/1", 300, PushUrgency.HIGH)

    @Test
    fun `VAPID 서명과 암호화된 본문으로 푸시 서비스에 보낸다`() {
        sender().send(listOf(1L), message)

        val headers = requests.single()
        assertTrue(headers.getValue("authorization").startsWith("vapid t="))
        assertEquals("aes128gcm", headers["content-encoding"])
        assertEquals("300", headers["ttl"])
        assertEquals("high", headers["urgency"])
        verify(repository, never()).deleteByEndpoint(endpoint)
    }

    @Test
    fun `푸시 서비스가 410이면 구독을 지운다`() {
        responseStatus = 410

        sender().send(listOf(1L), message)

        verify(repository).deleteByEndpoint(endpoint)
    }

    @Test
    fun `VAPID 키가 없으면 보내지 않는다`() {
        WebPushSender(WebPushProperties(), repository, ObjectMapper()).send(listOf(1L), message)

        assertTrue(requests.isEmpty())
    }
}
