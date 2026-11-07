package com.fullaccel.paircarserver.crew.domain

import com.fullaccel.paircarserver.global.entity.BaseEntity
import com.fullaccel.paircarserver.member.domain.Member
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.time.LocalTime

// 멤버가 크루에 참여함
@Entity
@Table(
    name = "crew_members",
    uniqueConstraints = [UniqueConstraint(name = "uk_crew_members_crew_member", columnNames = ["crew_id", "member_id"])],
)
class CrewMember(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false, updatable = false)
    val crew: Crew,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    val member: Member,

    @Column(nullable = false, updatable = false)
    val joinedAt: Instant = Instant.now(),
) : BaseEntity() {

    // 동승자로 탈 때 평소 탈 곳, 내릴 곳. 그날만 다르면 출석(CrewAttendance)에서 바꿈
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_place_id")
    var boardPlace: CrewPlace? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alight_place_id")
    var alightPlace: CrewPlace? = null
        protected set

    // 평소 픽업 시각 (KST). 운전자가 이 사람을 태우는 시각
    @Column
    var pickupTime: LocalTime? = null
        protected set

    fun changePlaces(boardPlace: CrewPlace?, alightPlace: CrewPlace?, pickupTime: LocalTime?) {
        this.boardPlace = boardPlace
        this.alightPlace = alightPlace
        this.pickupTime = pickupTime
    }
}
