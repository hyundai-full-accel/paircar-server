package com.fullaccel.paircarserver.member.api

import com.fullaccel.paircarserver.global.auth.LoginMember
import com.fullaccel.paircarserver.global.response.ApiResponse
import com.fullaccel.paircarserver.member.api.dto.BankAccountRequest
import com.fullaccel.paircarserver.member.api.dto.CreateMemberRequest
import com.fullaccel.paircarserver.member.api.dto.CreateMemberResponse
import com.fullaccel.paircarserver.member.application.MemberService
import com.fullaccel.paircarserver.member.application.MemberTokenService
import com.fullaccel.paircarserver.member.application.dto.BankAccountResult
import com.fullaccel.paircarserver.member.application.dto.MemberResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "멤버", description = "멤버, 기기 토큰, 정산 받을 계좌")
@RestController
@RequestMapping("/api/members")
class MemberController(
    private val memberService: MemberService,
    private val memberTokenService: MemberTokenService,
) {

    @Operation(
        summary = "멤버 생성 (차량 앱 첫 실행)",
        description = "멤버를 만들고 기기 토큰을 발급합니다. 토큰은 이 응답에서만 받을 수 있습니다. 인증이 필요 없습니다.",
    )
    @SecurityRequirements
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createMember(@Valid @RequestBody request: CreateMemberRequest): ApiResponse<CreateMemberResponse> {
        val member = memberService.createMember(request.nickname)
        val token = memberTokenService.issueToken(member.memberId)
        return ApiResponse.ok(CreateMemberResponse(member.memberId, member.nickname, token))
    }

    @Operation(summary = "내 정보")
    @GetMapping("/me")
    fun getMe(@LoginMember memberId: Long): ApiResponse<MemberResult> =
        ApiResponse.ok(memberService.getMember(memberId))

    @Operation(
        summary = "정산 받을 계좌 등록",
        description = "운전자는 운행을 시작하기 전에 계좌를 등록해야 합니다. 이미 있으면 수정합니다.",
    )
    @PutMapping("/me/bank-account")
    fun registerBankAccount(
        @LoginMember memberId: Long,
        @Valid @RequestBody request: BankAccountRequest,
    ): ApiResponse<BankAccountResult> =
        ApiResponse.ok(memberService.registerBankAccount(memberId, request.toCommand()))

    @Operation(summary = "내 계좌 조회", description = "등록하지 않았으면 data가 없습니다.")
    @GetMapping("/me/bank-account")
    fun getBankAccount(@LoginMember memberId: Long): ApiResponse<BankAccountResult?> =
        ApiResponse(success = true, data = memberService.getBankAccount(memberId))
}
