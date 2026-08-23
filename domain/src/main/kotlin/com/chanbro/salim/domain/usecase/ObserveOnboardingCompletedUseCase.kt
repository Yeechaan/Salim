package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 온보딩을 이미 봤는지 관찰. */
class ObserveOnboardingCompletedUseCase @Inject constructor(
    private val repository: OnboardingRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.completed
}
