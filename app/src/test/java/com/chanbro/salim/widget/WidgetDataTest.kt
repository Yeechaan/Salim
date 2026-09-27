package com.chanbro.salim.widget

import com.chanbro.salim.ui.dday.DDayRowUi
import com.chanbro.salim.ui.dday.upcoming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WidgetDataTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    private fun seoulMillis(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        Calendar.getInstance(seoul).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    @Test
    fun `다음 자정은 다음 날 0시다`() {
        val next = nextLocalMidnight(seoulMillis(2026, 9, 27, 21, 30), Calendar.getInstance(seoul))
        assertEquals(seoulMillis(2026, 9, 28), next)
    }

    @Test
    fun `정확히 자정이면 그다음 날 자정이다`() {
        val next = nextLocalMidnight(seoulMillis(2026, 9, 27), Calendar.getInstance(seoul))
        assertEquals(seoulMillis(2026, 9, 28), next)
    }

    @Test
    fun `월말 자정은 다음 달 1일로 넘어간다`() {
        val next = nextLocalMidnight(seoulMillis(2026, 12, 31, 23, 59), Calendar.getInstance(seoul))
        assertEquals(seoulMillis(2027, 1, 1), next)
    }

    @Test
    fun `위젯과 홈 카드는 지난 항목을 빼고 당일은 남긴다`() {
        fun row(id: String, dDay: String) = DDayRowUi(id, id, "", dDay, isAuto = false, repeatYearly = false)
        val rows = listOf(row("today", "D-DAY"), row("soon", "D-3"), row("past", "D+2"))

        assertEquals(listOf("today", "soon"), rows.upcoming().map { it.id })
    }

    @Test
    fun `D-DAY만 당일 강조 대상이다`() {
        fun row(dDay: String) = DDayRowUi("id", "결혼기념일", "", dDay, isAuto = false, repeatYearly = false)

        assertTrue(row("D-DAY").isToday)
        assertFalse(row("D-1").isToday)
    }
}
