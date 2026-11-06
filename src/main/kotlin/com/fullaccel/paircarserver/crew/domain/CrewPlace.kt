package com.fullaccel.paircarserver.crew.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant

// 크루의 장소 (○○아파트 정문, 회사, 운전자 집). 크루 멤버 누구나 등록하고 크루 전체가 같이 씀
// 운행의 출발지, 목적지, 탑승, 하차 지점이 모두 여기를 가리킴
@Entity
@Table(
    name = "crew_places",
    uniqueConstraints = [UniqueConstraint(name = "uk_crew_places_crew_name", columnNames = ["crew_id", "name"])],
)
class CrewPlace(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false, updatable = false)
    val crew: Crew,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = false, precision = 9, scale = 6)
    var latitude: BigDecimal,

    @Column(nullable = false, precision = 9, scale = 6)
    var longitude: BigDecimal,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity()
