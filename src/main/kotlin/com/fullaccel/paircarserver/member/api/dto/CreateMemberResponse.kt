package com.fullaccel.paircarserver.member.api.dto

import io.swagger.v3.oas.annotations.media.Schema

data class CreateMemberResponse(
    val memberId: Long,
    val nickname: String,
    @field:Schema(description = "기기 토큰. 이 응답에서만 한 번 받을 수 있으니 기기에 저장하세요")
    val token: String,
)
