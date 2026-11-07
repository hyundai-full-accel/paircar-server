package com.fullaccel.paircarserver.settlement.domain

import com.fullaccel.paircarserver.crew.domain.CrewMember
import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

// 영수증 기록 확인 요청 ("이 기록에 문제가 있나요?"). 영수증은 바꾸지 않고, 운전자가 보고 처리했다고 표시함
@Entity
@Table(name = "receipt_inquiries")
class ReceiptInquiry(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false, updatable = false)
    val receipt: Receipt,

    // 요청한 사람 (영수증의 내는 사람)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false, updatable = false)
    val requester: CrewMember,

    @Column(nullable = false, length = 500, updatable = false)
    val message: String,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity() {

    @Column
    var resolvedAt: Instant? = null
        protected set

    // 이미 처리됐으면 처음 시각 유지
    fun resolve() {
        if (resolvedAt == null) resolvedAt = Instant.now()
    }
}
