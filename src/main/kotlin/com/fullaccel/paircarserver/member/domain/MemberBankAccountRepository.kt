package com.fullaccel.paircarserver.member.domain

import org.springframework.data.jpa.repository.JpaRepository

interface MemberBankAccountRepository : JpaRepository<MemberBankAccount, Long>
