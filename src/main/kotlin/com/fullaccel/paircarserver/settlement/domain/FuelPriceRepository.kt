package com.fullaccel.paircarserver.settlement.domain

import org.springframework.data.jpa.repository.JpaRepository

interface FuelPriceRepository : JpaRepository<FuelPrice, Long>
