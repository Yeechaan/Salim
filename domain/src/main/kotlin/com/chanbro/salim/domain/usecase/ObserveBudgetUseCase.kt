package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Budget
import com.chanbro.salim.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 해당 연월의 예산을 관찰. */
class ObserveBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository,
) {
    operator fun invoke(year: Int, month: Int): Flow<Budget?> = repository.observe(year, month)
}
