package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.DDay
import com.chanbro.salim.domain.repository.DDayRepository
import javax.inject.Inject

/** 디데이 저장(신규 등록 / 수정 공용). */
class SaveDDayUseCase @Inject constructor(
    private val repository: DDayRepository,
) {
    suspend operator fun invoke(dDay: DDay) = repository.save(dDay)
}
