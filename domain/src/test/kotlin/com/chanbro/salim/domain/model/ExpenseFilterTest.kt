package com.chanbro.salim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseFilterTest {

    private val categories = DefaultCategories.all

    // 2026-08-03 00:00 UTC. 지출 시각은 "UTC 자정 + 시간" 규약이다.
    private val aug3 = 1_785_715_200_000L
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `조건이 없으면 전부 통과한다`() {
        val expenses = listOf(expense(), expense(memo = null))

        assertFalse(ExpenseFilter().isActive)
        assertEquals(expenses, ExpenseFilter().apply(expenses, categories))
    }

    @Test
    fun `검색어는 메모 부분일치이고 대소문자와 앞뒤 공백을 무시한다`() {
        val filter = ExpenseFilter(query = "  starbucks ")

        assertTrue(filter.matches(expense(memo = "강남 Starbucks"), categories))
        assertFalse(filter.matches(expense(memo = "이디야"), categories))
        assertFalse(filter.matches(expense(memo = null), categories))
    }

    @Test
    fun `공백뿐인 검색어는 조건이 아니다`() {
        assertFalse(ExpenseFilter(query = "   ").isActive)
        assertTrue(ExpenseFilter(query = "   ").matches(expense(memo = null), categories))
    }

    @Test
    fun `기간은 시작일 0시부터 종료일 끝까지 포함한다`() {
        val filter = ExpenseFilter(startDayUtc = aug3, endDayUtc = aug3 + day * 2)

        assertFalse(filter.matches(expense(spentAt = aug3 - 1), categories))
        assertTrue(filter.matches(expense(spentAt = aug3), categories))
        assertTrue(filter.matches(expense(spentAt = aug3 + day * 3 - 1), categories))
        assertFalse(filter.matches(expense(spentAt = aug3 + day * 3), categories))
    }

    @Test
    fun `한쪽만 정한 기간은 반대쪽이 열려 있다`() {
        assertTrue(ExpenseFilter(startDayUtc = aug3).matches(expense(spentAt = aug3 + day * 20), categories))
        assertTrue(ExpenseFilter(endDayUtc = aug3).matches(expense(spentAt = aug3 - day * 2), categories))
    }

    @Test
    fun `시작일이 종료일보다 늦으면 맞바꾼다`() {
        val filter = ExpenseFilter().withPeriod(aug3 + day, aug3)

        assertEquals(aug3, filter.startDayUtc)
        assertEquals(aug3 + day, filter.endDayUtc)
    }

    @Test
    fun `카테고리는 여러 개 중 하나만 맞으면 되고 id 없는 예전 지출은 이름으로 찾는다`() {
        val filter = ExpenseFilter(categoryIds = setOf("food", "culture"))

        assertTrue(filter.matches(expense(categoryId = "food"), categories))
        assertTrue(filter.matches(expense(categoryId = null, categoryName = "문화/여가"), categories))
        assertFalse(filter.matches(expense(categoryId = "cafe"), categories))
        assertFalse(filter.matches(expense(categoryId = "gone", categoryName = "반려동물"), categories))
    }

    @Test
    fun `지출자 조건은 미연결이면 버린다`() {
        val filter = ExpenseFilter(spenders = setOf(Spender.PARTNER))

        assertFalse(filter.matches(expense(spender = Spender.ME), categories))
        assertTrue(filter.matches(expense(spender = Spender.PARTNER), categories))
        assertEquals(ExpenseFilter(), filter.forConnection(connected = false))
        assertEquals(filter, filter.forConnection(connected = true))
    }

    @Test
    fun `조건을 모두 함께 만족해야 통과한다`() {
        val filter = ExpenseFilter(query = "점심", categoryIds = setOf("food"), spenders = setOf(Spender.ME))

        assertTrue(filter.matches(expense(memo = "점심 김밥", categoryId = "food"), categories))
        assertFalse(filter.matches(expense(memo = "점심 김밥", categoryId = "cafe"), categories))
        assertFalse(filter.matches(expense(memo = "저녁", categoryId = "food"), categories))
    }

    @Test
    fun `조건 비우기는 검색어를 남긴다`() {
        val filter = ExpenseFilter(query = "점심", startDayUtc = aug3, categoryIds = setOf("food"))

        assertEquals(ExpenseFilter(query = "점심"), filter.clearConditions())
    }

    private fun expense(
        memo: String? = "메모",
        spentAt: Long = aug3 + 60 * 60 * 1000,
        categoryId: String? = "food",
        categoryName: String = "식비",
        spender: Spender = Spender.ME,
    ) = Expense(
        id = "e",
        amount = 1_000,
        spentAtMillis = spentAt,
        spender = spender,
        categoryName = categoryName,
        memo = memo,
        createdAtMillis = 0,
        categoryId = categoryId,
    )
}
