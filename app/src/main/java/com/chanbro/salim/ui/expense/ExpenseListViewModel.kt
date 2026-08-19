package com.chanbro.salim.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.usecase.ObserveMonthExpensesUseCase
import com.chanbro.salim.ui.common.formatThousands
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

// UI 표시용 모델 (도메인 → 화면 매핑 결과). 아이콘/색은 화면에서 categoryName으로 결정.
data class ExpenseRowUi(
    val categoryName: String,
    val title: String,
    val meta: String,
    val amount: String,
)

data class ExpenseDayUi(
    val dateHeader: String,
    val rows: List<ExpenseRowUi>,
)

data class ExpenseListUiState(
    val year: Int = 2026,
    val month: Int = 8,
    val monthTotal: String = "0원",
    val days: List<ExpenseDayUi> = emptyList(),
)

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    observeMonth: ObserveMonthExpensesUseCase,
) : ViewModel() {

    private val yearMonth = MutableStateFlow(2026 to 8)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ExpenseListUiState> = yearMonth
        .flatMapLatest { (year, month) ->
            observeMonth(year, month).map { expenses -> toUiState(year, month, expenses) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpenseListUiState(),
        )

    fun setMonth(year: Int, month: Int) {
        yearMonth.value = year to month
    }

    private fun toUiState(year: Int, month: Int, expenses: List<Expense>): ExpenseListUiState {
        val total = expenses.sumOf { it.amount }
        val days = expenses
            .groupBy { dayStartUtc(it.spentAtMillis) }
            .entries
            .sortedByDescending { it.key }
            .map { (dayMillis, items) ->
                ExpenseDayUi(
                    dateHeader = formatDayHeader(dayMillis),
                    rows = items.map { it.toRowUi() },
                )
            }
        return ExpenseListUiState(
            year = year,
            month = month,
            monthTotal = "${formatThousands(total.toString())}원",
            days = days,
        )
    }

    private fun Expense.toRowUi() = ExpenseRowUi(
        categoryName = categoryName,
        title = memo?.takeIf { it.isNotBlank() } ?: categoryName,
        meta = "$categoryName · ${spender.label}",
        amount = "-${formatThousands(amount.toString())}원",
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
