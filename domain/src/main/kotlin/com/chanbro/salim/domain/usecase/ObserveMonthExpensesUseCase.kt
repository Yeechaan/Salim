package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 해당 연월의 지출 목록을 관찰. */
class ObserveMonthExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(year: Int, month: Int): Flow<List<Expense>> =
        repository.observeMonth(year, month)
}
