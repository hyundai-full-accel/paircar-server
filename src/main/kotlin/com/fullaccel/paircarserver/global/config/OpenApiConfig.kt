package com.fullaccel.paircarserver.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("페어카 크루 서버 API")
                .version("v1")
                .description(DESCRIPTION),
        )
        .components(
            Components().addSecuritySchemes(
                BEARER,
                SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").description("멤버 생성, 크루 참여 때 발급한 토큰"),
            ),
        )
        .addSecurityItem(SecurityRequirement().addList(BEARER))

    companion object {
        private const val BEARER = "bearer"

        private val DESCRIPTION = """
            ## 인증
            로그인 없이 기기 토큰으로 멤버를 구분합니다.
            - 차량 앱: `POST /api/members`로 멤버를 만들면 응답에 `token`이 한 번만 옵니다. 기기에 저장하세요.
            - 이후 모든 요청: `Authorization: Bearer {token}`

            ## 응답 형식
            - 성공: `{ "success": true, "data": ... }`
            - 실패: `{ "success": false, "error": { "code", "message", "fieldErrors" } }`
            - 클라이언트는 `error.code`로 분기합니다. 메시지는 바뀔 수 있습니다.

            ## 단위와 형식
            - 거리, 누적 주행거리(Odometer): km, 소수 1자리 (예: `12345.6`)
            - 연비: km/L (전기차는 km/kWh), 소수 2자리
            - 금액: 원
            - 시각: ISO-8601, 시간대 포함 (예: `2026-11-07T08:10:00+09:00`)
            - 운행일, 정산 월: KST 기준

            ## 운행 호출 순서 (차량 앱)
            1. 운행 시작 → 응답의 `waypoints`로 경유지 설정
            2. 탑승 감지마다 탑승 기록, 하차 감지마다 하차 기록
            3. 구간 업로드 (탑승 인원이 바뀌는 지점마다 나눈 구간)
            4. 운행 종료

            ## 재전송 (오프라인 버퍼)
            - 탑승, 하차, 구간, 종료는 같은 요청을 다시 보내도 결과가 같습니다.
            - 버퍼는 발생 순서대로 보내세요 (탑승 → 하차 → 구간 → 종료).
            - 5xx와 429는 잠시 뒤 재시도하세요. 그 밖의 4xx는 다시 보내도 실패합니다.

            ## 게이트웨이 (nginx) 오류
            - `TOO_MANY_REQUESTS` (429): 요청 제한 초과. 초대 코드는 IP당 분당 10회, 그 밖의 API는 초당 20회
            - `SERVICE_UNAVAILABLE` (503): 서버가 재시작 중이거나 응답하지 않음
        """.trimIndent()
    }
}
