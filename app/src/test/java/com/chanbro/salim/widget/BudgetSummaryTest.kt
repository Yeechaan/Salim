package com.chanbro.salim.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 예산 위젯 계산 — 홈 예산 카드와 같은 규칙인지 (widget.md 10-2). */
class BudgetSummaryTest {

    @Test
    fun `예산 안이면 남은 금액과 사용률`() {
        val s = BudgetSummary(month = 9, budget = 1_200_000, spent = 748_000)

        assertTrue(s.hasBudget)
        assertFalse(s.isOver)
        assertEquals(452_000L, s.remainOrOver)
        assertEquals(62, s.usedPercent)
        assertEquals(0.6233f, s.progress, 0.001f)
    }

    @Test
    fun `초과면 초과액이고 진행률은 가득`() {
        val s = BudgetSummary(month = 9, budget = 500_000, spent = 538_000)

        assertTrue(s.isOver)
        assertEquals(38_000L, s.remainOrOver)
        assertEquals(107, s.usedPercent)
        assertEquals(1f, s.progress)
    }

    @Test
    fun `딱 맞게 쓰면 초과가 아니다`() {
        val s = BudgetSummary(month = 9, budget = 500_000, spent = 500_000)

        assertFalse(s.isOver)
        assertEquals(0L, s.remainOrOver)
        assertEquals(100, s.usedPercent)
    }

    @Test
    fun `예산이 없거나 0이면 미설정`() {
        assertFalse(BudgetSummary(month = 9, budget = null, spent = 10_000).hasBudget)
        val zero = BudgetSummary(month = 9, budget = 0, spent = 10_000)
        assertFalse(zero.hasBudget)
        assertFalse(zero.isOver)
        assertEquals(0, zero.usedPercent)
    }
}
