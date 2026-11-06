package com.fullaccel.paircarserver.member.application

import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.dto.BankAccountCommand
import com.fullaccel.paircarserver.member.application.dto.BankAccountResult
import com.fullaccel.paircarserver.member.application.dto.MemberResult
import com.fullaccel.paircarserver.member.domain.Member
import com.fullaccel.paircarserver.member.domain.MemberBankAccount
import com.fullaccel.paircarserver.member.domain.MemberBankAccountRepository
import com.fullaccel.paircarserver.member.domain.MemberRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberServiceImpl(
    private val memberRepository: MemberRepository,
    private val memberBankAccountRepository: MemberBankAccountRepository,
) : MemberService {

    @Transactional
    override fun createMember(nickname: String): MemberResult =
        MemberResult.from(memberRepository.save(Member(nickname = nickname)))

    override fun getMember(memberId: Long): MemberResult =
        MemberResult.from(getMemberEntity(memberId))

    override fun getMemberEntity(memberId: Long): Member =
        memberRepository.findByIdOrNull(memberId)
            ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)

    @Transactional
    override fun registerBankAccount(memberId: Long, command: BankAccountCommand): BankAccountResult {
        val existing = memberBankAccountRepository.findByMemberId(memberId)
        if (existing != null) {
            existing.update(command.bankName, command.accountNumber, command.accountHolder)
            return BankAccountResult.from(existing)
        }
        val account = MemberBankAccount(
            member = getMemberEntity(memberId),
            bankName = command.bankName,
            accountNumber = command.accountNumber,
            accountHolder = command.accountHolder,
        )
        return BankAccountResult.from(memberBankAccountRepository.save(account))
    }

    override fun getBankAccount(memberId: Long): BankAccountResult? {
        getMemberEntity(memberId)
        return memberBankAccountRepository.findByMemberId(memberId)?.let(BankAccountResult::from)
    }
}
