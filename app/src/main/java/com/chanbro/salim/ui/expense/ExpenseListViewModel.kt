package com.chanbro.salim.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Category
import com.chanbro.salim.domain.model.CategoryLabel
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.DefaultCategories
import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.model.ExpenseFilter
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.model.labelOf
import com.chanbro.salim.domain.usecase.ObserveCategoriesUseCase
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import com.chanbro.salim.domain.usecase.ObserveMonthExpensesUseCase
import com.chanbro.salim.domain.usecase.ObserveSpenderNamesUseCase
import com.chanbro.salim.ui.common.currentYearMonth
import com.chanbro.salim.ui.common.formatWon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

// UI 표시용 모델 (도메인 → 화면 매핑 결과). 아이콘/색은 화면에서 iconKey/colorKey로 결정.
data class ExpenseRowUi(
    /** 탭 시 지출 수정 화면으로 넘길 id. (expense.md 4-1) */
    val id: String,
    val iconKey: String,
    val colorKey: String,
    val title: String,
    val meta: String,
    val amount: String,
)

data class ExpenseDayUi(
    val dateHeader: String,
    val rows: List<ExpenseRowUi>,
)

data class ExpenseListUiState(
    val year: Int = 0,
    val month: Int = 0,
    /** 검색/필터가 걸려 있으면 조건에 맞는 지출의 합계. (expense.md 4-1 검색/필터 적용 중 표시) */
    val monthTotal: String = "0원",
    val days: List<ExpenseDayUi> = emptyList(),
    /** 이 달에 지출이 하나라도 있는지 — 빈 상태 문구가 "이번 달 없음"과 "조건에 맞는 것 없음"으로 갈린다. */
    val monthHasExpenses: Boolean = false,
    /** 지금 적용된 조건. 미연결이면 지출자 조건은 이미 빠져 있다. */
    val filter: ExpenseFilter = ExpenseFilter(),
    /** 조건에 맞는 지출 건수. */
    val resultCount: Int = 0,
    /** 미연결이면 필터 시트의 지출자 구역과 지출자 조건 칩을 숨긴다. */
    val connected: Boolean = false,
    val names: SpenderNames = SpenderNames(),
    val categories: List<Category> = DefaultCategories.all,
) {
    /** 필터 시트 날짜 선택 범위 — 선택한 달의 1일 / 말일 (UTC 자정 millis). */
    val monthStartUtc: Long get() = monthDayUtc(year, month, 1)
    val monthEndUtc: Long get() = monthDayUtc(year, month + 1, 1) - DAY_MILLIS

    /** 조건 칩으로 보여줄 카테고리. 지운 카테고리 id는 칩을 만들 수 없어 뺀다. */
    val selectedCategories: List<Category> get() = categories.filter { it.id in filter.categoryIds }
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

private fun monthDayUtc(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        // month가 13이어도 Calendar가 다음 해 1월로 넘겨준다.
        set(year, month - 1, day)
    }.timeInMillis

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    observeMonth: ObserveMonthExpensesUseCase,
    observeSpenderNames: ObserveSpenderNamesUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observeConnection: ObserveConnectionUseCase,
) : ViewModel() {

    private val yearMonth = MutableStateFlow(currentYearMonth())

    /** 검색어 + 필터. 화면을 오가도(수정 화면 → 복귀) 유지된다. */
    private val filter = MutableStateFlow(ExpenseFilter())

    /** 달을 결과와 함께 들고 다닌다 — 따로 combine하면 달이 먼저 바뀌어 헤더와 목록이 어긋난다. */
    private data class MonthExpenses(val year: Int, val month: Int, val expenses: List<Expense>)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthExpenses = yearMonth
        .flatMapLatest { (year, month) ->
            observeMonth(year, month).map { MonthExpenses(year, month, it) }
        }

    val uiState: StateFlow<ExpenseListUiState> =
        combine(
            monthExpenses,
            observeSpenderNames(),
            observeCategories(),
            observeConnection(),
            filter,
        ) { month, names, categories, connection, filter ->
            val connected = connection is Connection.Connected
            toUiState(month, names, categories, connected, filter.forConnection(connected))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            // 첫 스냅샷 전에도 헤더가 이번 달로 보이게 한다.
            initialValue = currentYearMonth().let { (y, m) -> ExpenseListUiState(year = y, month = m) },
        )

    /** 달을 바꾸면 기간 조건만 푼다 — 다른 달의 날짜라 맞을 수가 없다. (PRD 4 전체보기) */
    fun setMonth(year: Int, month: Int) {
        filter.update { it.withPeriod(null, null) }
        yearMonth.value = year to month
    }

    fun setQuery(query: String) {
        filter.update { it.copy(query = query) }
    }

    /** 필터 시트 "적용하기". 검색어는 시트가 다루지 않으므로 지금 값을 유지한다. */
    fun applyConditions(conditions: ExpenseFilter) {
        filter.update {
            it.copy(
                categoryIds = conditions.categoryIds,
                spenders = conditions.spenders,
            ).withPeriod(conditions.startDayUtc, conditions.endDayUtc)
        }
    }

    fun clearPeriod() {
        filter.update { it.withPeriod(null, null) }
    }

    fun removeCategory(id: String) {
        filter.update { it.copy(categoryIds = it.categoryIds - id) }
    }

    fun removeSpender(spender: Spender) {
        filter.update { it.copy(spenders = it.spenders - spender) }
    }

    /** 결과 없음의 "필터 초기화" — 검색어까지 모두 비운다. (expense.md 4-1 상태 분기) */
    fun clearAll() {
        filter.value = ExpenseFilter()
    }

    private fun toUiState(
        month: MonthExpenses,
        names: SpenderNames,
        categories: List<Category>,
        connected: Boolean,
        filter: ExpenseFilter,
    ): ExpenseListUiState {
        val shown = filter.apply(month.expenses, categories)
        val days = shown
            .groupBy { dayStartUtc(it.spentAtMillis) }
            .entries
            .sortedByDescending { it.key }
            .map { (dayMillis, items) ->
                ExpenseDayUi(
                    dateHeader = formatDayHeader(dayMillis),
                    rows = items.map { it.toRowUi(names, categories.labelOf(it)) },
                )
            }
        return ExpenseListUiState(
            year = month.year,
            month = month.month,
            monthTotal = formatWon(shown.sumOf { it.amount }),
            days = days,
            monthHasExpenses = month.expenses.isNotEmpty(),
            filter = filter,
            resultCount = shown.size,
            connected = connected,
            names = names,
            categories = categories,
        )
    }

    private fun Expense.toRowUi(names: SpenderNames, category: CategoryLabel) = ExpenseRowUi(
        id = id,
        iconKey = category.iconKey,
        colorKey = category.colorKey,
        title = memo?.takeIf { it.isNotBlank() } ?: category.name,
        meta = "${category.name} · ${names.labelOf(spender)}",
        amount = "-${formatWon(amount)}",
    )

    private fun dayStartUtc(utcMillis: Long): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun formatDayHeader(dayUtcMillis: Long): String =
        SimpleDateFormat("M월 d일 (E)", Locale.KOREAN).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(dayUtcMillis))
}
