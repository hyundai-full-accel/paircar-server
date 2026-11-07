package com.fullaccel.paircarserver.car.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

// 차량 (Pleos Connect 탑재 차량)
@Entity
@Table(name = "cars")
class Car(
    // Pleos Connect 차량 식별값
    @Column(nullable = false, unique = true, length = 64, updatable = false)
    val vehicleKey: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var fuelType: FuelType,

    // 입력 연비 (km/L, EV는 km/kWh). 실측 연비를 못 쓰는 차종만 입력
    @Column(precision = 5, scale = 2)
    var manualFuelEconomy: BigDecimal? = null,

    // 직접 입력 유가 (원/L, EV는 원/kWh). 집 충전 단가처럼 공공 평균과 다를 때만. 비어 있으면 fuel_prices 사용
    @Column
    var manualFuelPriceWon: Int? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity() {

    fun update(fuelType: FuelType, manualFuelEconomy: BigDecimal?, manualFuelPriceWon: Int?) {
        this.fuelType = fuelType
        this.manualFuelEconomy = manualFuelEconomy
        this.manualFuelPriceWon = manualFuelPriceWon
    }
}
