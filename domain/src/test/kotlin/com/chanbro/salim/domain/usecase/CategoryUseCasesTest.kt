package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Category
import com.chanbro.salim.domain.model.CategoryColors
import com.chanbro.salim.domain.model.CategoryNameError
import com.chanbro.salim.domain.model.DefaultCategories
import com.chanbro.salim.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryUseCasesTest {

    private val repository = FakeCategoryRepository(DefaultCategories.all)

    @Test
    fun `추가한 항목은 더보기 맨 끝에 붙는다`() = runBlocking {
        val error = AddCategoryUseCase(repository)("  반려동물 ")

        assertNull(error)
        val added = repository.current.single { it.name == "반려동물" }
        assertFalse(added.fixed)
        assertEquals(Category.CUSTOM_ICON, added.iconKey)
        assertEquals(DefaultCategories.all.maxOf { it.order } + 1, added.order)
    }

    @Test
    fun `추가한 항목은 아직 아무도 안 쓰는 색을 받는다`() = runBlocking {
        AddCategoryUseCase(repository)("반려동물")

        // 기본 11개가 쓰지 않는 팔레트의 마지막 색
        assertEquals("olive", repository.current.single { it.name == "반려동물" }.colorKey)
    }

    @Test
    fun `팔레트가 다 차면 가장 덜 쓰인 색을 받는다`() = runBlocking {
        val add = AddCategoryUseCase(repository)
        add("반려동물") // olive
        add("육아")     // 모든 색이 1번씩 쓰였으니 팔레트 첫 색

        assertEquals("peach", repository.current.single { it.name == "육아" }.colorKey)
    }

    @Test
    fun `같은 색 계열 항목을 고정으로 올리면 그 항목 색만 다른 계열로 바꾼다`() = runBlocking {
        // 의료/건강(lilac)은 고정 항목 문화(lavender)와 같은 보라 계열
        SwapFixedCategoryUseCase(repository)(fixedId = "cafe", moreId = "health")

        val fixedFamilies = repository.current.filter { it.fixed }.map { CategoryColors.familyOf(it.colorKey) }
        assertEquals(DefaultCategories.FIXED_COUNT, fixedFamilies.toSet().size)
        assertEquals("lavender", repository.current.single { it.id == "culture" }.colorKey)
    }

    @Test
    fun `계열이 겹치지 않으면 고정으로 올려도 색은 그대로다`() = runBlocking {
        SwapFixedCategoryUseCase(repository)(fixedId = "cafe", moreId = "transport")

        assertEquals("mint", repository.current.single { it.id == "transport" }.colorKey)
    }

    @Test
    fun `빈 이름 긴 이름 겹치는 이름은 추가하지 않는다`() = runBlocking {
        val add = AddCategoryUseCase(repository)

        assertEquals(CategoryNameError.BLANK, add("   "))
        assertEquals(CategoryNameError.TOO_LONG, add("아주아주긴카테고리이름"))
        assertEquals(CategoryNameError.DUPLICATE, add(" 식비"))
        assertEquals(DefaultCategories.all, repository.current)
    }

    @Test
    fun `이름을 바꾸면 같은 id에 새 이름이 저장된다`() = runBlocking {
        val error = RenameCategoryUseCase(repository)("food", "외식")

        assertNull(error)
        assertEquals("외식", repository.current.single { it.id == "food" }.name)
    }

    @Test
    fun `자기 이름 그대로 저장은 중복이 아니지만 다른 항목 이름은 중복이다`() = runBlocking {
        val rename = RenameCategoryUseCase(repository)

        assertNull(rename("food", "식비"))
        assertEquals(CategoryNameError.DUPLICATE, rename("food", "카페"))
    }

    @Test
    fun `고정 교체는 두 항목의 구역과 순서를 맞바꿔 고정 개수를 유지한다`() = runBlocking {
        SwapFixedCategoryUseCase(repository)(fixedId = "cafe", moreId = "transport")

        val cafe = repository.current.single { it.id == "cafe" }
        val transport = repository.current.single { it.id == "transport" }
        assertTrue(transport.fixed)
        assertEquals(1, transport.order)
        assertFalse(cafe.fixed)
        assertEquals(5, cafe.order)
        assertEquals(DefaultCategories.FIXED_COUNT, repository.current.count { it.fixed })
    }

    @Test
    fun `고정끼리나 더보기끼리는 교체하지 않는다`() = runBlocking {
        val swap = SwapFixedCategoryUseCase(repository)

        swap(fixedId = "food", moreId = "cafe")
        swap(fixedId = "transport", moreId = "living")

        assertEquals(DefaultCategories.all, repository.current)
    }
}

/** save가 id 기준으로 덮어쓰는, Firestore 문서 set과 같은 동작. */
private class FakeCategoryRepository(initial: List<Category>) : CategoryRepository {
    private val state = MutableStateFlow(initial)
    val current: List<Category> get() = state.value

    override fun observe(): Flow<List<Category>> = state

    override suspend fun save(categories: List<Category>) {
        val byId = categories.associateBy { it.id }
        val replaced = state.value.map { byId[it.id] ?: it }
        val added = categories.filter { new -> state.value.none { it.id == new.id } }
        state.value = (replaced + added).sortedBy { it.order }
    }
}
