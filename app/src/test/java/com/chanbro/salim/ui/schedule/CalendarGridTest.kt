package com.chanbro.salim.ui.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarGridTest {

    @Test
    fun `월 일수를 정확히 센다`() {
        assertEquals(31, daysInMonth(2026, 8))
        assertEquals(30, daysInMonth(2026, 9))
        assertEquals(28, daysInMonth(2026, 2))
        assertEquals(29, daysInMonth(2028, 2))  // 윤년
    }

    @Test
    fun `1일 요일 인덱스가 맞다`() {
        // 2026-08-01은 토요일 → 6
        assertEquals(6, firstDayOfWeekIndex(2026, 8))
        // 2026-11-01은 일요일 → 0
        assertEquals(0, firstDayOfWeekIndex(2026, 11))
    }

    @Test
    fun `격자는 항상 7의 배수이고 앞은 요일만큼 비어 있다`() {
        val cells = monthCells(2026, 8)
        assertEquals(0, cells.size % DAYS_IN_WEEK)
        // 1일이 토요일이므로 앞 6칸은 빈 칸
        repeat(6) { i -> assertNull(cells[i].dayOfMonth) }
        assertEquals(1, cells[6].dayOfMonth)
    }

    @Test
    fun `1일이 일요일이면 앞 빈 칸이 없다`() {
        val cells = monthCells(2026, 11)
        assertEquals(1, cells.first().dayOfMonth)
    }

    @Test
    fun `날짜 칸의 millis가 하루씩 증가한다`() {
        val cells = monthCells(2026, 8).filter { it.dayOfMonth != null }
        assertEquals(31, cells.size)
        val first = cells.first().dateMillis!!
        assertEquals(first + 30 * DAY_MILLIS, cells.last().dateMillis)
        assertEquals(startOfMonth(2026, 8), first)
    }

    @Test
    fun `주 단위로 자르면 각 주가 7칸이다`() {
        monthWeeks(2026, 2).forEach { week -> assertEquals(DAYS_IN_WEEK, week.size) }
        monthWeeks(2028, 2).forEach { week -> assertEquals(DAYS_IN_WEEK, week.size) }
    }

    @Test
    fun `시각 표기는 오전 오후를 구분한다`() {
        assertEquals("오전 12:00", formatMinuteOfDay(0))
        assertEquals("오전 9:05", formatMinuteOfDay(9 * 60 + 5))
        assertEquals("오후 12:30", formatMinuteOfDay(12 * 60 + 30))
        assertEquals("오후 7:00", formatMinuteOfDay(19 * 60))
        assertEquals("오후 11:59", formatMinuteOfDay(23 * 60 + 59))
    }
}
