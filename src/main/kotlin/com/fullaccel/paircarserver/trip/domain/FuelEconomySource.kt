package com.fullaccel.paircarserver.trip.domain

// 연비 출처. 영수증 표시 문구가 바뀜
enum class FuelEconomySource {
    MEASURED, // 차량 실측
    MANUAL,   // 입력 연비 (실측 미지원 차종. 영수증에 "입력 연비"로 표시)
}
