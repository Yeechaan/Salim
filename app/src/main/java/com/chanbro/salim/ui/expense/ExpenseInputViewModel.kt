package com.chanbro.salim.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Category
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.DefaultCategories
import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.model.findFor
import com.chanbro.salim.domain.usecase.AddExpenseUseCase
import com.chanbro.salim.domain.usecase.DeleteExpenseUseCase
import com.chanbro.salim.domain.usecase.GetExpenseUseCase
import com.chanbro.salim.domain.usecase.ObserveCategoriesUseCase
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import com.chanbro.salim.domain.usecase.ObserveSpenderNamesUseCase
import com.chanbro.salim.domain.usecase.UpdateExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

/** 수정 화면 프리필 값. (expense.md 4-3) */
data class ExpenseInitial(
    val amount: Long,
    val dateUtcMillis: Long,
    val hour24: Int,
    val minute: Int,
    val spender: Spender,
    /** 목록에서 찾지 못한 카테고리면 null — 화면은 첫 고정 칩으로 둔다. */
    val categoryId: String?,
    val memo: String,
)

/** 수정 모드 로딩 상태. 추가 모드는 처음부터 [Ready]. */
sealed interface ExpenseLoad {
    data object Loading : ExpenseLoad
    data class Ready(val initial: ExpenseInitial?) : ExpenseLoad
    /** 상대가 먼저 지웠거나 조회에 실패했다 — 화면을 닫는다. */
    data object Missing : ExpenseLoad
}

@HiltViewModel
class ExpenseInputViewModel @Inject constructor(
    private val addExpense: AddExpenseUseCase,
    private val getExpense: GetExpenseUseCase,
    private val updateExpense: UpdateExpenseUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
    private val observeCategories: ObserveCategoriesUseCase,
    observeConnection: ObserveConnectionUseCase,
    observeSpenderNames: ObserveSpenderNamesUseCase,
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

    private val _load = MutableStateFlow<ExpenseLoad>(ExpenseLoad.Ready(initial = null))
    val load: StateFlow<ExpenseLoad> = _load.asStateFlow()

    private var editingId: String? = null
    private var createdAtMillis: Long = 0L

    /** 수정 모드 진입 시 1회 호출. expenseId가 null이면 추가 모드. */
    fun load(expenseId: String?) {
        if (expenseId == null || editingId == expenseId) return
        editingId = expenseId
        _load.value = ExpenseLoad.Loading
        viewModelScope.launch {
            val expense = runCatching { getExpense(expenseId) }.getOrNull()
            if (expense == null) {
                _load.value = ExpenseLoad.Missing
                return@launch
            }
            createdAtMillis = expense.createdAtMillis
            val categories = observeCategories().first()
            _load.value = ExpenseLoad.Ready(expense.toInitial(categories))
        }
    }

    /**
     * 입력값으로 지출 1건을 저장. 수정 모드면 같은 id에 덮어쓰고 등록 시각은 유지한다.
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
        val id = editingId
        val expense = Expense(
            id = id ?: UUID.randomUUID().toString(),
            amount = amount,
            spentAtMillis = dateUtcMillis + hour24 * HOUR_MILLIS + minute * MINUTE_MILLIS,
            spender = spender,
            categoryName = category.name,
            memo = memo.trim().ifBlank { null },
            createdAtMillis = if (id != null) createdAtMillis else System.currentTimeMillis(),
            categoryId = category.id,
        )
        viewModelScope.launch {
            if (id != null) updateExpense(expense) else addExpense(expense)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = editingId ?: return
        viewModelScope.launch {
            deleteExpense(id)
            onDone()
        }
    }

    /** 저장할 때 합쳤던 날짜(UTC 자정)와 시각을 다시 나눈다. */
    private fun Expense.toInitial(categories: List<Category>): ExpenseInitial {
        val dateUtc = spentAtMillis - Math.floorMod(spentAtMillis, DAY_MILLIS)
        val timeOfDay = spentAtMillis - dateUtc
        return ExpenseInitial(
            amount = amount,
            dateUtcMillis = dateUtc,
            hour24 = (timeOfDay / HOUR_MILLIS).toInt(),
            minute = ((timeOfDay % HOUR_MILLIS) / MINUTE_MILLIS).toInt(),
            spender = spender,
            categoryId = categories.findFor(this)?.id,
            memo = memo.orEmpty(),
        )
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
        const val HOUR_MILLIS = 60 * MINUTE_MILLIS
        const val DAY_MILLIS = 24 * HOUR_MILLIS
    }
}
