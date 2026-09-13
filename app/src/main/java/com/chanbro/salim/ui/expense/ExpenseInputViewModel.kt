package com.chanbro.salim.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Category
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.DefaultCategories
import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.usecase.AddExpenseUseCase
import com.chanbro.salim.domain.usecase.ObserveCategoriesUseCase
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import com.chanbro.salim.domain.usecase.ObserveSpenderNamesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ExpenseInputUiState(
    /** 미연결이면 지출자 줄 자체를 숨긴다 — 모든 지출이 본인 것이다. (expense.md 4-2) */
    val connected: Boolean = false,
    /** 지출자 칩에 쓸 이름. 프로필을 비워 두면 "나"/"배우자"로 떨어진다. */
    val names: SpenderNames = SpenderNames(),
    /** 설정 > 카테고리 수정에서 정한 목록. 불러오기 전에도 칩이 비지 않게 기본 목록으로 시작한다. */
    val categories: List<Category> = DefaultCategories.all,
) {
    /** 칩으로 바로 고르는 항목. (PRD 4) */
    val fixedCategories: List<Category> get() = categories.filter { it.fixed }

    /** "+더보기" 시트에 나오는 항목. */
    val moreCategories: List<Category> get() = categories.filterNot { it.fixed }
}

@HiltViewModel
class ExpenseInputViewModel @Inject constructor(
    private val addExpense: AddExpenseUseCase,
    observeConnection: ObserveConnectionUseCase,
    observeSpenderNames: ObserveSpenderNamesUseCase,
    observeCategories: ObserveCategoriesUseCase,
) : ViewModel() {

    val uiState: StateFlow<ExpenseInputUiState> = combine(
        observeConnection(),
        observeSpenderNames(),
        observeCategories(),
    ) { connection, names, categories ->
        ExpenseInputUiState(
            connected = connection is Connection.Connected,
            names = names,
            categories = categories,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpenseInputUiState(),
    )

    /**
     * 입력값으로 지출 1건을 저장.
     * @param dateUtcMillis DatePicker의 UTC-자정 millis
     * @param hour24 0~23, @param minute 0~59 (시간 부분을 UTC 오프셋으로 합산)
     * @param onDone 저장 완료 후 호출(예: 뒤로가기)
     */
    fun save(
        amount: Long,
        spender: Spender,
        category: Category,
        memo: String,
        dateUtcMillis: Long,
        hour24: Int,
        minute: Int,
        onDone: () -> Unit,
    ) {
        val spentAt = dateUtcMillis + hour24 * 3_600_000L + minute * 60_000L
        val expense = Expense(
            id = UUID.randomUUID().toString(),
            amount = amount,
            spentAtMillis = spentAt,
            spender = spender,
            categoryName = category.name,
            memo = memo.trim().ifBlank { null },
            createdAtMillis = System.currentTimeMillis(),
            categoryId = category.id,
        )
        viewModelScope.launch {
            addExpense(expense)
            onDone()
        }
    }
}
