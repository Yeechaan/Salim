package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Schedule
import com.chanbro.salim.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 해당 연월의 일정 목록을 관찰. */
class ObserveMonthSchedulesUseCase @Inject constructor(
    private val repository: ScheduleRepository,
) {
    operator fun invoke(year: Int, month: Int): Flow<List<Schedule>> =
        repository.observeMonth(year, month)
}
