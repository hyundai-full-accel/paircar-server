package com.fullaccel.paircarserver.global.auth

// 토큰 원문으로 멤버 ID를 찾음. 구현은 member 패키지에 있음 (global이 도메인에 의존하지 않도록 인터페이스만 둠)
fun interface TokenAuthenticator {

    // 유효하지 않으면 null
    fun authenticate(rawToken: String): Long?
}
