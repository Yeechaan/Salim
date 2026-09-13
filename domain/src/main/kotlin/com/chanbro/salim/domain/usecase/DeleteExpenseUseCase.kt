package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.repository.ExpenseRepository
import javax.inject.Inject

/** 지출 삭제. (PRD 4 지출 수정 — 확인 팝업 후) */
class DeleteExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}
