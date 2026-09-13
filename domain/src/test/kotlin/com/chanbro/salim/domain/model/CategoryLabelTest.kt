package com.chanbro.salim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryLabelTest {

    private val categories = DefaultCategories.all.map { if (it.id == "food") it.copy(name = "외식") else it }

    @Test
    fun `id가 있는 지출은 카테고리의 지금 이름으로 보인다`() {
        val label = categories.labelOf(expense(categoryId = "food", categoryName = "식비"))

        assertEquals(CategoryLabel("외식", "food", "peach"), label)
    }

    @Test
    fun `id가 없는 예전 지출은 저장된 이름 그대로 보이고 모양만 맞춘다`() {
        assertEquals(CategoryLabel("식비", "food", "peach"), categories.labelOf(expense(null, "식비")))
        assertEquals(CategoryLabel("문화/여가", "culture", "lavender"), categories.labelOf(expense(null, "문화/여가")))
    }

    @Test
    fun `목록에 없는 id는 저장된 이름으로 떨어진다`() {
        val label = categories.labelOf(expense(categoryId = "gone", categoryName = "반려동물"))

        assertEquals(CategoryLabel("반려동물", "etc", "warmgray"), label)
    }

    @Test
    fun `기본 카테고리는 모두 색이 다르다`() {
        val colors = DefaultCategories.all.map { it.colorKey }

        assertEquals(colors.size, colors.toSet().size)
        assertEquals(true, CategoryColors.keys.containsAll(colors))
    }

    @Test
    fun `기본 고정 5개는 색 계열까지 다르다`() {
        val families = DefaultCategories.all.filter { it.fixed }.map { CategoryColors.familyOf(it.colorKey) }

        assertEquals(DefaultCategories.FIXED_COUNT, families.toSet().size)
    }

    @Test
    fun `수정할 카테고리는 id로 먼저 찾는다`() {
        assertEquals("food", categories.findFor(expense(categoryId = "food", categoryName = "아무 이름"))?.id)
    }

    @Test
    fun `id가 없는 예전 지출은 이름이나 예전 기본 이름으로 찾는다`() {
        assertEquals("cafe", categories.findFor(expense(null, "카페"))?.id)
        assertEquals("culture", categories.findFor(expense(null, "문화/여가"))?.id)
        // 이름을 "외식"으로 바꾼 뒤에도 예전 "식비" 지출은 식비 카테고리로 잡힌다.
        assertEquals("food", categories.findFor(expense(null, "식비"))?.id)
    }

    @Test
    fun `어디에도 없는 카테고리는 찾지 못한다`() {
        assertEquals(null, categories.findFor(expense(categoryId = "gone", categoryName = "반려동물")))
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
