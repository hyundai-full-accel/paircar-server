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

    // 입력 연비 (km/L, EV는 km/kWh). 실측 연비를 못 쓰는 차종만 크루 생성 때 입력
    @Column(precision = 5, scale = 2)
    var manualFuelEconomy: BigDecimal? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity()
