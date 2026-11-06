package com.fullaccel.paircarserver.member.domain

import org.springframework.data.jpa.repository.JpaRepository

interface MemberTokenRepository : JpaRepository<MemberToken, Long> {

    fun findByTokenHash(tokenHash: String): MemberToken?
}
