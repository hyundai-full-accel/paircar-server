package com.fullaccel.paircarserver.car.domain

// 연료 종류. 평균 유가를 고르고 연비, 유가 단위를 정할 때 사용
enum class FuelType {
    GASOLINE, // 휘발유
    DIESEL,   // 경유
    LPG,      // LPG
    HYBRID,   // 하이브리드 (휘발유 기준으로 계산)
    EV,       // 전기 (연비 km/kWh, 유가 원/kWh)
}
