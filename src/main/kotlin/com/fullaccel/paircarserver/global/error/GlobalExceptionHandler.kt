package com.fullaccel.paircarserver.global.error

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fullaccel.paircarserver.global.response.ApiResponse
import com.fullaccel.paircarserver.global.response.ErrorBody
import com.fullaccel.paircarserver.global.response.FieldErrorBody
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

// 모든 예외를 ApiResponse 실패 형식으로 바꿈
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ResponseEntity<ApiResponse<Any>> {
        log.debug("Business exception: {} {}", e.errorCode, e.message)
        return respond(e.errorCode, e.message, data = e.data)
    }

    // @Valid 요청 본문 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleNotValid(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Any>> {
        val fieldErrors = e.bindingResult.fieldErrors.map {
            FieldErrorBody(field = it.field, reason = it.defaultMessage ?: "올바르지 않은 값입니다.")
        }
        return respond(ErrorCode.INVALID_INPUT, fieldErrors = fieldErrors)
    }

    // @RequestParam, @PathVariable 등에 붙인 검증 실패
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleMethodValidation(e: HandlerMethodValidationException): ResponseEntity<ApiResponse<Any>> {
        val fieldErrors = e.parameterValidationResults.flatMap { result ->
            val field = result.methodParameter.parameterName ?: "parameter"
            result.resolvableErrors.map { FieldErrorBody(field = field, reason = it.defaultMessage ?: "올바르지 않은 값입니다.") }
        }
        return respond(ErrorCode.INVALID_INPUT, fieldErrors = fieldErrors)
    }

    // JSON 문법 오류, 타입 불일치, Kotlin non-null 필드 누락
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(e: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Any>> {
        val cause = e.cause
        if (cause is MismatchedInputException && cause.path.isNotEmpty()) {
            val field = cause.path.joinToString(".") { it.fieldName ?: "[${it.index}]" }
            return respond(ErrorCode.INVALID_JSON, fieldErrors = listOf(FieldErrorBody(field, "값이 없거나 형식이 올바르지 않습니다.")))
        }
        return respond(ErrorCode.INVALID_JSON)
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(e: MissingRequestHeaderException): ResponseEntity<ApiResponse<Any>> =
        respond(ErrorCode.INVALID_INPUT, fieldErrors = listOf(FieldErrorBody(e.headerName, "필수 헤더입니다.")))

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(e: MissingServletRequestParameterException): ResponseEntity<ApiResponse<Any>> =
        respond(ErrorCode.INVALID_INPUT, fieldErrors = listOf(FieldErrorBody(e.parameterName, "필수 파라미터입니다.")))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Any>> =
        respond(ErrorCode.INVALID_INPUT, fieldErrors = listOf(FieldErrorBody(e.name, "형식이 올바르지 않습니다.")))

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(e: NoResourceFoundException): ResponseEntity<ApiResponse<Any>> =
        respond(ErrorCode.RESOURCE_NOT_FOUND)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException): ResponseEntity<ApiResponse<Any>> =
        respond(ErrorCode.METHOD_NOT_ALLOWED)

    // unique, FK 제약 위반. 서비스에서 먼저 검증하지 못한 경쟁 상황(동시 요청 등)에서 발생
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(e: DataIntegrityViolationException): ResponseEntity<ApiResponse<Any>> {
        log.warn("Data integrity violation", e)
        return respond(ErrorCode.DATA_CONFLICT)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ApiResponse<Any>> {
        log.error("Unexpected exception", e)
        return respond(ErrorCode.INTERNAL_ERROR)
    }

    private fun respond(
        errorCode: ErrorCode,
        message: String = errorCode.message,
        fieldErrors: List<FieldErrorBody> = emptyList(),
        data: Any? = null,
    ): ResponseEntity<ApiResponse<Any>> =
        ResponseEntity
            .status(errorCode.status)
            .body(ApiResponse.fail(ErrorBody(code = errorCode.name, message = message, fieldErrors = fieldErrors), data))
}
