package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Schedule
import com.chanbro.salim.domain.repository.ScheduleRepository
import javax.inject.Inject

/** 수정 화면 프리필용 단건 조회. */
class GetScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(id: String): Schedule? = repository.get(id)
}
