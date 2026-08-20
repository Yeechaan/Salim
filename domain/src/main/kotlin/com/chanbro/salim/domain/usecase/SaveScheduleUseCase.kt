package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Schedule
import com.chanbro.salim.domain.repository.ScheduleRepository
import javax.inject.Inject

/** 일정 저장(등록 / 수정 공용). */
class SaveScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(schedule: Schedule) = repository.save(schedule)
}
