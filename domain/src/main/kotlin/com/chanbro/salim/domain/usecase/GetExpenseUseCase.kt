package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.repository.ExpenseRepository
import javax.inject.Inject

/** 수정 화면 프리필용 단건 조회. */
class GetExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(id: String): Expense? = repository.get(id)
}
