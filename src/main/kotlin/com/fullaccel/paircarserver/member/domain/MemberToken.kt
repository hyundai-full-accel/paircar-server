package com.fullaccel.paircarserver.member.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

// 멤버의 인증 토큰 (기기, 브라우저마다 1개. 개별 폐기 가능)
@Entity
@Table(
    name = "member_tokens",
    indexes = [Index(name = "idx_member_tokens_member_id", columnList = "member_id")],
)
class MemberToken(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    val member: Member,

    // 토큰 해시 (원문은 저장하지 않음)
    @Column(nullable = false, unique = true, length = 64, updatable = false)
    val tokenHash: String,

    @Column(nullable = false, updatable = false)
    val expiresAt: Instant,

    // 채워지면 사용 불가
    @Column
    var revokedAt: Instant? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity()
