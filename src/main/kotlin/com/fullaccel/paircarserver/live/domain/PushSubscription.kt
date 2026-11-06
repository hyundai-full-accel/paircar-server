package com.fullaccel.paircarserver.live.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

// 브라우저 Web Push 구독 (기기, 브라우저마다 1행)
// 알림 채널 데이터라 멤버 엔티티와 연관관계를 맺지 않고 memberId 값만 가짐 (live 모듈이 member 모듈에 묶이지 않도록)
@Entity
@Table(
    name = "push_subscriptions",
    indexes = [Index(name = "idx_push_subscriptions_member_id", columnList = "member_id")],
)
class PushSubscription(
    @Column(nullable = false)
    var memberId: Long,

    // 브라우저 푸시 서비스 주소. 브라우저마다 고유
    @Column(nullable = false, unique = true, length = 1024)
    val endpoint: String,

    // 메시지 암호화용 브라우저 공개키 (base64url)
    @Column(nullable = false, length = 255)
    var p256dh: String,

    // 메시지 암호화용 인증 비밀값 (base64url)
    @Column(nullable = false, length = 255)
    var auth: String,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = createdAt,
) : BaseEntity() {

    // 같은 브라우저를 다른 멤버가 쓰게 되었거나 키가 바뀐 경우
    fun renew(memberId: Long, p256dh: String, auth: String) {
        this.memberId = memberId
        this.p256dh = p256dh
        this.auth = auth
        this.updatedAt = Instant.now()
    }
}
