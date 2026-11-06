package com.fullaccel.paircarserver.member.application

import com.fullaccel.paircarserver.IntegrationTest
import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.member.application.dto.BankAccountCommand
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNull

@IntegrationTest
class MemberServiceTest(
    @Autowired private val memberService: MemberService,
) {

    @Test
    fun `멤버를 만들고 조회한다`() {
        val created = memberService.createMember("수아")

        val found = memberService.getMember(created.memberId)

        assertEquals("수아", found.nickname)
    }

    @Test
    fun `없는 멤버를 조회하면 MEMBER_NOT_FOUND`() {
        val e = assertThrows<BusinessException> { memberService.getMember(999_999L) }

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, e.errorCode)
    }

    @Test
    fun `계좌가 없으면 null`() {
        val member = memberService.createMember("지훈")

        assertNull(memberService.getBankAccount(member.memberId))
    }

    @Test
    fun `계좌를 등록하고 다시 등록하면 수정된다`() {
        val member = memberService.createMember("지훈")

        memberService.registerBankAccount(member.memberId, BankAccountCommand("○○은행", "3333-01-1234567", "지훈"))
        memberService.registerBankAccount(member.memberId, BankAccountCommand("△△은행", "1002-123-456789", "지훈"))

        val account = memberService.getBankAccount(member.memberId)
        assertEquals("△△은행", account?.bankName)
        assertEquals("1002-123-456789", account?.accountNumber)
    }

    @Test
    fun `없는 멤버에 계좌를 등록하면 MEMBER_NOT_FOUND`() {
        val e = assertThrows<BusinessException> {
            memberService.registerBankAccount(999_999L, BankAccountCommand("○○은행", "3333", "지훈"))
        }

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, e.errorCode)
    }
}
