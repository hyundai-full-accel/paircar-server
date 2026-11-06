package com.fullaccel.paircarserver.car.domain

import org.springframework.data.jpa.repository.JpaRepository

interface CarRepository : JpaRepository<Car, Long>
