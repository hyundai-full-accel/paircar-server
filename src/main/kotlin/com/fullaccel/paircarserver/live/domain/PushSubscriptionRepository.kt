package com.fullaccel.paircarserver.live.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface PushSubscriptionRepository : JpaRepository<PushSubscription, Long> {

    fun findByEndpoint(endpoint: String): PushSubscription?

    fun findAllByMemberIdIn(memberIds: Collection<Long>): List<PushSubscription>

    fun findAllByMemberId(memberId: Long): List<PushSubscription>

    // 만료된 구독 정리 (비동기 발송 스레드에서도 호출하므로 자체 트랜잭션)
    @Transactional
    fun deleteByEndpoint(endpoint: String)
}
