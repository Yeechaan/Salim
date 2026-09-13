package com.chanbro.salim.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Category
import com.chanbro.salim.domain.model.CategoryNameError
import com.chanbro.salim.domain.model.validateCategoryName
import com.chanbro.salim.domain.usecase.AddCategoryUseCase
import com.chanbro.salim.domain.usecase.ObserveCategoriesUseCase
import com.chanbro.salim.domain.usecase.RenameCategoryUseCase
import com.chanbro.salim.domain.usecase.SwapFixedCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryEditUiState(
    val fixed: List<Category>,
    val more: List<Category>,
) {
    val all: List<Category> get() = fixed + more
}

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    observeCategories: ObserveCategoriesUseCase,
    private val addCategory: AddCategoryUseCase,
    private val renameCategory: RenameCategoryUseCase,
    private val swapFixedCategory: SwapFixedCategoryUseCase,
) : ViewModel() {

    /** null이면 아직 불러오는 중. */
    val uiState: StateFlow<CategoryEditUiState?> = observeCategories()
        .map { all -> CategoryEditUiState(fixed = all.filter { it.fixed }, more = all.filterNot { it.fixed }) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /**
     * 입력 창에서 확인을 누를 때 화면이 가진 목록으로 먼저 검증한다. 통과하면 창을 바로 닫고
     * 저장은 뒤에서 한다 — 오프라인에서도 창이 서버 응답을 기다리며 멈춰 있지 않게.
     * 저장 직전에 UseCase가 한 번 더 검증한다.
     */
    fun validate(name: String, excludingId: String? = null): CategoryNameError? =
        validateCategoryName(name, uiState.value?.all.orEmpty(), excludingId)

    fun add(name: String) {
        viewModelScope.launch { addCategory(name) }
    }

    fun rename(id: String, name: String) {
        viewModelScope.launch { renameCategory(id, name) }
    }

    fun swapFixed(fixedId: String, moreId: String) {
        viewModelScope.launch { swapFixedCategory(fixedId, moreId) }
    }
}
