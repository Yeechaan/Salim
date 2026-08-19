package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.repository.ExpenseRepository
import javax.inject.Inject

/** 지출 1건 추가. */
class AddExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(expense: Expense) = repository.add(expense)
}
