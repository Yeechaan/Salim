package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.DDay
import com.chanbro.salim.domain.repository.DDayRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 디데이 목록을 관찰. */
class ObserveDDaysUseCase @Inject constructor(
    private val repository: DDayRepository,
) {
    operator fun invoke(): Flow<List<DDay>> = repository.observeAll()
}
