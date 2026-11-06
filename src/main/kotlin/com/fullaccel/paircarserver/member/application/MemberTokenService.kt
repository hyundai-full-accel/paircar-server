package com.fullaccel.paircarserver.member.application

import com.fullaccel.paircarserver.global.auth.TokenAuthenticator

// 기기 토큰 발급과 인증. 토큰 원문은 발급할 때 한 번만 반환하고 DB에는 SHA-256 해시만 저장함
interface MemberTokenService : TokenAuthenticator {

    fun issueToken(memberId: Long): String
}
