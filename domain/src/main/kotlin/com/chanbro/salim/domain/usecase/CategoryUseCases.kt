package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Category
import com.chanbro.salim.domain.model.CategoryNameError
import com.chanbro.salim.domain.model.validateCategoryName
import com.chanbro.salim.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/** 카테고리 목록 관찰. */
class ObserveCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    operator fun invoke(): Flow<List<Category>> = repository.observe()
}

/** 더보기 항목 추가. 새 항목은 더보기 구역 맨 끝에 붙는다. (PRD 7) */
class AddCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    /** @return 검증에 걸리면 그 사유, 저장했으면 null */
    suspend operator fun invoke(name: String): CategoryNameError? {
        val existing = repository.observe().first()
        validateCategoryName(name, existing)?.let { return it }
        val category = Category(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            iconKey = Category.CUSTOM_ICON,
            fixed = false,
            order = (existing.maxOfOrNull { it.order } ?: -1) + 1,
        )
        repository.save(listOf(category))
        return null
    }
}

/** 이름 수정. 고정/더보기 구분 없이 모든 항목에 쓸 수 있다. (PRD 7) */
class RenameCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    /** @return 검증에 걸리면 그 사유, 저장했거나 대상이 사라졌으면 null */
    suspend operator fun invoke(id: String, name: String): CategoryNameError? {
        val existing = repository.observe().first()
        validateCategoryName(name, existing, excludingId = id)?.let { return it }
        val target = existing.firstOrNull { it.id == id } ?: return null
        repository.save(listOf(target.copy(name = name.trim())))
        return null
    }
}

/**
 * 더보기 항목을 고정으로 올리고, 고른 고정 항목을 더보기로 내린다. (PRD 7)
 * 고정 개수를 5개로 유지하려고 한쪽만 옮기는 경로는 두지 않는다. 순서도 맞바꿔서,
 * 올라온 항목이 내려간 항목의 칩 자리에 그대로 들어간다.
 */
class SwapFixedCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(fixedId: String, moreId: String) {
        val existing = repository.observe().first()
        val fixed = existing.firstOrNull { it.id == fixedId && it.fixed } ?: return
        val more = existing.firstOrNull { it.id == moreId && !it.fixed } ?: return
        repository.save(
            listOf(
                fixed.copy(fixed = false, order = more.order),
                more.copy(fixed = true, order = fixed.order),
            ),
        )
    }
}
