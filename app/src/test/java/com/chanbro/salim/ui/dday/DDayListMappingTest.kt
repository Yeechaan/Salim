package com.chanbro.salim.ui.dday

import com.chanbro.salim.domain.model.DDay
import com.chanbro.salim.domain.model.DDaySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 도메인 목록 → 화면 행 매핑(정렬 포함) 검증. */
class DDayListMappingTest {

    private val today = utcDate(2026, 8, 20)

    private fun dDay(
        id: String,
        title: String,
        dateMillis: Long,
        repeatYearly: Boolean = false,
        source: DDaySource = DDaySource.MANUAL,
    ) = DDay(id, title, dateMillis, repeatYearly, source, createdAtMillis = 0L)

    @Test
    fun `다가올 항목이 남은 일수 오름차순으로 정렬된다`() {
        val rows = listOf(
            dDay("c", "이사", utcDate(2026, 11, 20)),
            dDay("a", "결혼기념일", utcDate(2026, 8, 31)),
            dDay("b", "제주 여행", utcDate(2026, 9, 15)),
        ).toRows(today)

        assertEquals(listOf("a", "b", "c"), rows.map { it.id })
        assertEquals(listOf("D-11", "D-26", "D-92"), rows.map { it.dDayText })
    }

    @Test
    fun `이미 지난 1회성 항목은 뒤로 밀리고 최근 것부터 온다`() {
        val rows = listOf(
            dDay("old", "지난 여행", utcDate(2026, 7, 1)),
            dDay("recent", "지난 주말", utcDate(2026, 8, 17)),
            dDay("soon", "제주 여행", utcDate(2026, 9, 15)),
        ).toRows(today)

        assertEquals(listOf("soon", "recent", "old"), rows.map { it.id })
        assertEquals("D+3", rows[1].dDayText)
    }

    @Test
    fun `매년 반복은 다음 기념일 기준으로 정렬되고 날짜도 그 해로 표시된다`() {
        val rows = listOf(
            dDay("trip", "제주 여행", utcDate(2026, 9, 15)),
            dDay("bday", "배우자 생일", utcDate(1993, 8, 25), repeatYearly = true),
        ).toRows(today)

        // 생일(8/25, D-5)이 여행(9/15, D-26)보다 앞
        assertEquals(listOf("bday", "trip"), rows.map { it.id })
        assertEquals("D-5", rows[0].dDayText)
        assertEquals("2026.08.25", rows[0].dateText)
        assertTrue(rows[0].repeatYearly)
    }

    @Test
    fun `source가 AUTO면 자동 항목으로 표시된다`() {
        val rows = listOf(
            dDay("auto", "결혼기념일", utcDate(2020, 8, 31), repeatYearly = true, source = DDaySource.AUTO),
            dDay("manual", "이사", utcDate(2026, 11, 20)),
        ).toRows(today)

        assertTrue(rows.first { it.id == "auto" }.isAuto)
        assertTrue(!rows.first { it.id == "manual" }.isAuto)
    }
}
