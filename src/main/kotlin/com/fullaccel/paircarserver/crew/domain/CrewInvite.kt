package com.fullaccel.paircarserver.crew.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

// 크루 초대 코드
@Entity
@Table(
    name = "crew_invites",
    indexes = [Index(name = "idx_crew_invites_crew_id", columnList = "crew_id")],
)
class CrewInvite(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false, updatable = false)
    val crew: Crew,

    // 초대 링크에 들어가는 코드
    @Column(nullable = false, unique = true, length = 32, updatable = false)
    val code: String,

    // 지나면 참여 불가
    @Column(nullable = false, updatable = false)
    val expiresAt: Instant,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity()
