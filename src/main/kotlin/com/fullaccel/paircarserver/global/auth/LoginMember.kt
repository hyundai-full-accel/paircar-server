package com.fullaccel.paircarserver.global.auth

import io.swagger.v3.oas.annotations.Parameter

// 컨트롤러 파라미터(Long)에 붙이면 Authorization 토큰의 멤버 ID가 들어옴. 토큰이 없거나 틀리면 UNAUTHORIZED
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(hidden = true)
annotation class LoginMember
