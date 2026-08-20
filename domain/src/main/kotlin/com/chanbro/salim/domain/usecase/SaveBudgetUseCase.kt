package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Budget
import com.chanbro.salim.domain.repository.BudgetRepository
import javax.inject.Inject

/** 월 예산 설정. */
class SaveBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository,
) {
    suspend operator fun invoke(budget: Budget) = repository.save(budget)
}
