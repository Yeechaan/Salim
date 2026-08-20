package com.chanbro.salim.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Budget
import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.usecase.ObserveBudgetUseCase
import com.chanbro.salim.domain.usecase.ObserveMonthExpensesUseCase
import com.chanbro.salim.domain.usecase.SaveBudgetUseCase
import com.chanbro.salim.ui.common.formatThousands
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

// UI 표시용 모델 (도메인 → 화면 매핑 결과).
data class CategorySpendUi(
    val name: String,
    val amount: String,
    val ratio: Float,
)

data class TransactionUi(
    val title: String,
    val meta: String,
    val amount: String,
)

data class HomeUiState(
    val year: Int = 0,
    val month: Int = 0,
    val monthSpent: Long = 0L,
    val budgetAmount: Long? = null,   // null = 예산 미설정
    val categories: List<CategorySpendUi> = emptyList(),
    val recent: List<TransactionUi> = emptyList(),
) {
    val spentText: String get() = "${formatThousands(monthSpent.toString()).ifEmpty { "0" }}원"
    val budgetText: String? get() = budgetAmount?.let { "${formatThousands(it.toString())}원" }

    /** 예산 대비 사용 비율. 미설정이면 null, 초과하면 1f로 잘라 진행바에 사용. */
    val usedRatio: Float?
        get() = budgetAmount?.takeIf { it > 0 }?.let { (monthSpent.toFloat() / it).coerceIn(0f, 1f) }

    val usedPercent: Int?
        get() = budgetAmount?.takeIf { it > 0 }?.let { (monthSpent * 100 / it).toInt() }

    /** 남은 예산 문구. 초과 시 초과액으로 바꿔 보여준다. */
    val remainText: String?
        get() = budgetAmount?.let {
            val remain = it - monthSpent
            if (remain >= 0) "남은 예산 ${formatThousands(remain.toString()).ifEmpty { "0" }}원"
            else "예산 ${formatThousands((-remain).toString())}원 초과"
        }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeMonthExpenses: ObserveMonthExpensesUseCase,
    observeBudget: ObserveBudgetUseCase,
    private val saveBudget: SaveBudgetUseCase,
) : ViewModel() {

    private val yearMonth = MutableStateFlow(currentYearMonth())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = yearMonth
        .flatMapLatest { (year, month) ->
            combine(
                observeMonthExpenses(year, month),
                observeBudget(year, month),
            ) { expenses, budget ->
                toUiState(year, month, expenses, budget)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = currentYearMonth().let { (y, m) -> HomeUiState(year = y, month = m) },
        )

    fun setMonth(year: Int, month: Int) {
        yearMonth.value = year to month
    }

    fun setBudget(amount: Long) {
        val (year, month) = yearMonth.value
        viewModelScope.launch {
            saveBudget(Budget(year, month, amount))
        }
    }

    private fun toUiState(
        year: Int,
        month: Int,
        expenses: List<Expense>,
        budget: Budget?,
    ): HomeUiState {
        val total = expenses.sumOf { it.amount }
        val categories = expenses
            .groupBy { it.categoryName }
            .map { (name, items) -> name to items.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .map { (name, amount) ->
                CategorySpendUi(
                    name = name,
                    amount = "${formatThousands(amount.toString()).ifEmpty { "0" }}원",
                    ratio = if (total > 0) amount.toFloat() / total else 0f,
                )
            }
        val recent = expenses
            .sortedByDescending { it.createdAtMillis }
            .take(3)
            .map { it.toTransactionUi() }

        return HomeUiState(
            year = year,
            month = month,
            monthSpent = total,
            budgetAmount = budget?.amount,
            categories = categories,
            recent = recent,
        )
    }

    private fun Expense.toTransactionUi() = TransactionUi(
        title = memo?.takeIf { it.isNotBlank() } ?: categoryName,
        meta = "$categoryName · ${spender.label} · ${formatShortDate(spentAtMillis)}",
        amount = "-${formatThousands(amount.toString()).ifEmpty { "0" }}원",
    )

    private fun formatShortDate(utcMillis: Long): String =
        SimpleDateFormat("M/d", Locale.KOREAN).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(utcMillis))

    private fun currentYearMonth(): Pair<Int, Int> = Calendar.getInstance().let {
        it.get(Calendar.YEAR) to it.get(Calendar.MONTH) + 1
    }
}
