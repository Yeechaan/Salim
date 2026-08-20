package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.DDay
import com.chanbro.salim.domain.repository.DDayRepository
import javax.inject.Inject

/** 수정 화면 프리필용 단건 조회. */
class GetDDayUseCase @Inject constructor(
    private val repository: DDayRepository,
) {
    suspend operator fun invoke(id: String): DDay? = repository.get(id)
}
