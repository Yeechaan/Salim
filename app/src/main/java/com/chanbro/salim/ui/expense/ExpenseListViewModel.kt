package com.chanbro.salim.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Category
import com.chanbro.salim.domain.model.CategoryLabel
import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.model.labelOf
import com.chanbro.salim.domain.usecase.ObserveCategoriesUseCase
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

// UI 표시용 모델 (도메인 → 화면 매핑 결과). 아이콘/색은 화면에서 iconKey로 결정.
data class ExpenseRowUi(
    val iconKey: String,
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
    val monthTotal: String = "0원",
    val days: List<ExpenseDayUi> = emptyList(),
)

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    observeMonth: ObserveMonthExpensesUseCase,
    observeSpenderNames: ObserveSpenderNamesUseCase,
    observeCategories: ObserveCategoriesUseCase,
) : ViewModel() {

    private val yearMonth = MutableStateFlow(currentYearMonth())

    /** 달을 결과와 함께 들고 다닌다 — 따로 combine하면 달이 먼저 바뀌어 헤더와 목록이 어긋난다. */
    private data class MonthExpenses(val year: Int, val month: Int, val expenses: List<Expense>)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthExpenses = yearMonth
        .flatMapLatest { (year, month) ->
            observeMonth(year, month).map { MonthExpenses(year, month, it) }
        }

    val uiState: StateFlow<ExpenseListUiState> =
        combine(monthExpenses, observeSpenderNames(), observeCategories()) { month, names, categories ->
            toUiState(month.year, month.month, month.expenses, names, categories)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            // 첫 스냅샷 전에도 헤더가 이번 달로 보이게 한다.
            initialValue = currentYearMonth().let { (y, m) -> ExpenseListUiState(year = y, month = m) },
        )

    fun setMonth(year: Int, month: Int) {
        yearMonth.value = year to month
    }

    private fun toUiState(
        year: Int,
        month: Int,
        expenses: List<Expense>,
        names: SpenderNames,
        categories: List<Category>,
    ): ExpenseListUiState {
        val total = expenses.sumOf { it.amount }
        val days = expenses
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
            year = year,
            month = month,
            monthTotal = formatWon(total),
            days = days,
        )
    }

    private fun Expense.toRowUi(names: SpenderNames, category: CategoryLabel) = ExpenseRowUi(
        iconKey = category.iconKey,
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
