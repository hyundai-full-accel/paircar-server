package com.fullaccel.paircarserver.crew.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import com.fullaccel.paircarserver.member.domain.Member
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

// 멤버가 크루에 참여함
@Entity
@Table(
    name = "crew_members",
    uniqueConstraints = [UniqueConstraint(name = "uk_crew_members_crew_member", columnNames = ["crew_id", "member_id"])],
)
class CrewMember(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false, updatable = false)
    val crew: Crew,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    val member: Member,

    @Column(nullable = false, updatable = false)
    val joinedAt: Instant = Instant.now(),
) : BaseEntity()
