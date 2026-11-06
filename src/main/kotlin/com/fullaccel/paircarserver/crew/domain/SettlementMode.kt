package com.fullaccel.paircarserver.crew.domain

// 크루 정산 방식 (기술명세 T5)
enum class SettlementMode {
    FAIR,          // 공정 분할 (기본): 구간 비용을 구간 탑승 인원으로 나누고 우회 거리 비용을 더함
    EQUAL,         // 균등 분할: 운행 비용을 탑승 인원으로 똑같이 나눔
    FIXED_MONTHLY, // 월 정액: 크루가 정한 금액을 매달 청구
}
