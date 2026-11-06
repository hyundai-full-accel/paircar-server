package com.fullaccel.paircarserver.car.domain

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

// 멤버가 차를 소유하거나 운전함
@Entity
@Table(
    name = "member_cars",
    uniqueConstraints = [UniqueConstraint(name = "uk_member_cars_member_car", columnNames = ["member_id", "car_id"])],
)
class MemberCar(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false, updatable = false)
    val car: Car,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity()
