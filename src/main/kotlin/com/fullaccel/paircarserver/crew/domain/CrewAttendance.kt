package com.fullaccel.paircarserver.crew.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.time.LocalDate

// 크루 멤버의 그날 출석. 기본은 "탐"이라 안 타는 날이나 그날만 다른 곳에서 타는 날만 행이 생김
@Entity
@Table(
    name = "crew_attendances",
    uniqueConstraints = [UniqueConstraint(name = "uk_crew_attendances_member_date", columnNames = ["crew_member_id", "service_date"])],
)
class CrewAttendance(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_member_id", nullable = false, updatable = false)
    val crewMember: CrewMember,

    // 운행일 (KST)
    @Column(nullable = false, updatable = false)
    val serviceDate: LocalDate,

    attending: Boolean,

    boardPlace: CrewPlace?,
) : BaseEntity() {

    @Column(nullable = false)
    var attending: Boolean = attending
        protected set

    // 그날만 다른 탈 곳. 비어 있으면 크루 멤버의 기본 탈 곳
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_place_id")
    var boardPlace: CrewPlace? = boardPlace
        protected set

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    // 바뀌었으면 true
    fun change(attending: Boolean, boardPlace: CrewPlace?): Boolean {
        if (this.attending == attending && this.boardPlace?.id == boardPlace?.id) return false
        this.attending = attending
        this.boardPlace = boardPlace
        this.updatedAt = Instant.now()
        return true
    }
}
