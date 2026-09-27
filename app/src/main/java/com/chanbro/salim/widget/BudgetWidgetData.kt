package com.chanbro.salim.widget

import com.chanbro.salim.domain.usecase.ObserveBudgetUseCase
import com.chanbro.salim.domain.usecase.ObserveMonthExpensesUseCase
import com.chanbro.salim.ui.common.currentYearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 예산 위젯에 필요한 이번 달 요약 (PRD 10-2).
 * 계산은 홈 예산 카드(HomeUiState)와 같다 — 지출자 구분 없이 이번 달 전체 지출 ÷ 이번 달 예산.
 */
internal data class BudgetSummary(
    val month: Int,
    /** null 또는 0 이하 = 예산 미설정. */
    val budget: Long?,
    val spent: Long,
) {
    val hasBudget: Boolean get() = budget != null && budget > 0

    private val budgetOrZero: Long get() = budget?.takeIf { it > 0 } ?: 0L

    val isOver: Boolean get() = hasBudget && spent > budgetOrZero

    /** 남은 금액. 초과면 초과액 (항상 0 이상). */
    val remainOrOver: Long get() = kotlin.math.abs(budgetOrZero - spent)

    val usedPercent: Int get() = if (hasBudget) (spent * 100 / budgetOrZero).toInt() else 0

    /** 진행률 바 채움 비율. 초과하면 가득(1f). */
    val progress: Float get() = if (hasBudget) (spent.toFloat() / budgetOrZero).coerceIn(0f, 1f) else 0f
}

/** 지금 달 기준으로 구독한다. 달이 바뀌면 WidgetUpdater.restart()가 새로 붙는다. */
internal fun budgetSummary(
    observeMonthExpenses: ObserveMonthExpensesUseCase,
    observeBudget: ObserveBudgetUseCase,
): Flow<BudgetSummary> {
    val (year, month) = currentYearMonth()
    return combine(observeMonthExpenses(year, month), observeBudget(year, month)) { expenses, budget ->
        BudgetSummary(month = month, budget = budget?.amount, spent = expenses.sumOf { it.amount })
    }
}
