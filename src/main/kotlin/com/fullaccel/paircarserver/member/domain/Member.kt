package com.fullaccel.paircarserver.member.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

// 사용자 (운전자, 동승자 공통. 역할은 운행마다 정해짐)
@Entity
@Table(name = "members")
class Member(
    // 크루 안에서 보이는 이름 (TTS "수아님 탑승했어요"에 사용)
    @Column(nullable = false, length = 30)
    var nickname: String,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity()
