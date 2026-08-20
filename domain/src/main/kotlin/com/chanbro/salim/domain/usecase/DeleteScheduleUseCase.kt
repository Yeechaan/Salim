package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.repository.ScheduleRepository
import javax.inject.Inject

/** 일정 삭제. */
class DeleteScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}
