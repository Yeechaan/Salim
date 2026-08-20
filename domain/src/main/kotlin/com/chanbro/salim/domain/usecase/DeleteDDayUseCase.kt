package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.repository.DDayRepository
import javax.inject.Inject

/** 디데이 삭제. */
class DeleteDDayUseCase @Inject constructor(
    private val repository: DDayRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}
