package com.fullaccel.paircarserver.member.application

import com.fullaccel.paircarserver.member.application.dto.BankAccountCommand
import com.fullaccel.paircarserver.member.application.dto.BankAccountResult
import com.fullaccel.paircarserver.member.application.dto.MemberResult
import com.fullaccel.paircarserver.member.domain.Member

interface MemberService {

    fun createMember(nickname: String): MemberResult

    fun getMember(memberId: Long): MemberResult

    // 다른 패키지 서비스가 연관관계를 맺을 때 쓰는 엔티티 조회. 트랜잭션 안에서만 사용
    fun getMemberEntity(memberId: Long): Member

    // 계좌가 있으면 수정, 없으면 등록
    fun registerBankAccount(memberId: Long, command: BankAccountCommand): BankAccountResult

    fun getBankAccount(memberId: Long): BankAccountResult?
}
