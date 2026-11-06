package com.fullaccel.paircarserver.global.auth

import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class LoginMemberArgumentResolver(
    private val tokenAuthenticator: TokenAuthenticator,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(LoginMember::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long {
        val header = webRequest.getHeader(HttpHeaders.AUTHORIZATION)
        val token = header?.takeIf { it.startsWith(BEARER_PREFIX) }?.removePrefix(BEARER_PREFIX)?.trim()
        if (token.isNullOrEmpty()) throw BusinessException(ErrorCode.UNAUTHORIZED)
        return tokenAuthenticator.authenticate(token) ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
