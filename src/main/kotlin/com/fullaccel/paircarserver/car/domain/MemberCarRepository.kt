package com.fullaccel.paircarserver.car.domain

import org.springframework.data.jpa.repository.JpaRepository

interface MemberCarRepository : JpaRepository<MemberCar, Long>
