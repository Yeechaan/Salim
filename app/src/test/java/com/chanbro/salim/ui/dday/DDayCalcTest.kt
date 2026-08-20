package com.chanbro.salim.ui.dday

import org.junit.Assert.assertEquals
import org.junit.Test

class DDayCalcTest {

    private val today = utcDate(2026, 8, 20)

    @Test
    fun `1회성 항목은 남은 일수를 그대로 센다`() {
        assertEquals(11L, daysUntil(utcDate(2026, 8, 31), today))
        assertEquals(0L, daysUntil(today, today))
        assertEquals(-3L, daysUntil(utcDate(2026, 8, 17), today))
    }

    @Test
    fun `1회성 항목은 반복하지 않고 원래 날짜를 쓴다`() {
        val past = utcDate(2026, 8, 17)
        assertEquals(past, nextOccurrence(past, today, repeatYearly = false))
    }

    @Test
    fun `매년 반복은 아직 안 지났으면 올해 날짜`() {
        val anniversary = utcDate(2020, 8, 31)
        assertEquals(utcDate(2026, 8, 31), nextOccurrence(anniversary, today, repeatYearly = true))
    }

    @Test
    fun `매년 반복은 이미 지났으면 내년 날짜`() {
        val birthday = utcDate(1993, 8, 10)
        assertEquals(utcDate(2027, 8, 10), nextOccurrence(birthday, today, repeatYearly = true))
    }

    @Test
    fun `매년 반복은 당일이면 오늘을 유지한다`() {
        val sameDay = utcDate(2000, 8, 20)
        assertEquals(today, nextOccurrence(sameDay, today, repeatYearly = true))
        assertEquals(0L, daysUntil(nextOccurrence(sameDay, today, repeatYearly = true), today))
    }

    @Test
    fun `배지 문구는 당일 미래 과거를 구분한다`() {
        assertEquals("D-DAY", dDayLabel(0))
        assertEquals("D-12", dDayLabel(12))
        assertEquals("D+3", dDayLabel(-3))
    }

    @Test
    fun `연말을 넘어가는 반복도 내년으로 넘긴다`() {
        val yearEnd = utcDate(2020, 1, 1)
        val dec = utcDate(2026, 12, 25)
        assertEquals(utcDate(2027, 1, 1), nextOccurrence(yearEnd, dec, repeatYearly = true))
        assertEquals(7L, daysUntil(nextOccurrence(yearEnd, dec, repeatYearly = true), dec))
    }

    @Test
    fun `2월 29일 기념일은 평년에 2월 28일로 당긴다`() {
        val leapDay = utcDate(2024, 2, 29)
        assertEquals(utcDate(2026, 2, 28), nextOccurrence(leapDay, utcDate(2026, 1, 1), repeatYearly = true))
    }

    @Test
    fun `2월 29일 기념일은 윤년엔 그대로 29일`() {
        val leapDay = utcDate(2024, 2, 29)
        assertEquals(utcDate(2028, 2, 29), nextOccurrence(leapDay, utcDate(2028, 1, 1), repeatYearly = true))
    }

    @Test
    fun `평년 2월 28일 당일이면 D-DAY로 본다`() {
        val leapDay = utcDate(2024, 2, 29)
        val feb28 = utcDate(2026, 2, 28)
        assertEquals(0L, daysUntil(nextOccurrence(leapDay, feb28, repeatYearly = true), feb28))
    }

    @Test
    fun `평년 2월 28일이 지나면 내년으로 넘어간다`() {
        val leapDay = utcDate(2024, 2, 29)
        val mar1 = utcDate(2026, 3, 1)
        assertEquals(utcDate(2027, 2, 28), nextOccurrence(leapDay, mar1, repeatYearly = true))
    }

    @Test
    fun `날짜 표기는 점 구분 8자리`() {
        assertEquals("2026.08.31", formatDotDate(utcDate(2026, 8, 31)))
    }
}
