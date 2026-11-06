package com.fullaccel.paircarserver.member.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant

// 멤버의 정산 받을 계좌 (운전자만 등록. 멤버당 최대 1개)
@Entity
@Table(name = "member_bank_accounts")
class MemberBankAccount(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true, updatable = false)
    val member: Member,

    @Column(nullable = false, length = 30)
    var bankName: String,

    @Column(nullable = false, length = 30)
    var accountNumber: String,

    @Column(nullable = false, length = 30)
    var accountHolder: String,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = createdAt,
) : BaseEntity()
