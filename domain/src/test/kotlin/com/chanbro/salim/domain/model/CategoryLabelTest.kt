package com.chanbro.salim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryLabelTest {

    private val categories = DefaultCategories.all.map { if (it.id == "food") it.copy(name = "외식") else it }

    @Test
    fun `id가 있는 지출은 카테고리의 지금 이름으로 보인다`() {
        val label = categories.labelOf(expense(categoryId = "food", categoryName = "식비"))

        assertEquals(CategoryLabel("외식", "food"), label)
    }

    @Test
    fun `id가 없는 예전 지출은 저장된 이름 그대로 보이고 모양만 맞춘다`() {
        assertEquals(CategoryLabel("식비", "food"), categories.labelOf(expense(null, "식비")))
        assertEquals(CategoryLabel("문화/여가", "culture"), categories.labelOf(expense(null, "문화/여가")))
    }

    @Test
    fun `목록에 없는 id는 저장된 이름으로 떨어진다`() {
        val label = categories.labelOf(expense(categoryId = "gone", categoryName = "반려동물"))

        assertEquals(CategoryLabel("반려동물", "etc"), label)
    }

    private fun expense(categoryId: String?, categoryName: String) = Expense(
        id = "e1",
        amount = 1000,
        spentAtMillis = 0,
        spender = Spender.ME,
        categoryName = categoryName,
        memo = null,
        createdAtMillis = 0,
        categoryId = categoryId,
    )
}
