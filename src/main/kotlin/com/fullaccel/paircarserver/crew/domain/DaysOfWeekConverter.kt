package com.fullaccel.paircarserver.crew.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.DayOfWeek

// 요일 집합 <-> 비트 (월=1, 화=2, 수=4, ... 일=64)
@Converter
class DaysOfWeekConverter : AttributeConverter<Set<DayOfWeek>, Int> {

    override fun convertToDatabaseColumn(days: Set<DayOfWeek>?): Int =
        days.orEmpty().fold(0) { bits, day -> bits or bit(day) }

    override fun convertToEntityAttribute(bits: Int?): Set<DayOfWeek> =
        DayOfWeek.entries.filterTo(sortedSetOf()) { (bits ?: 0) and bit(it) != 0 }

    private fun bit(day: DayOfWeek) = 1 shl (day.value - 1)
}
