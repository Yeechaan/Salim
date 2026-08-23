package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.repository.OnboardingRepository
import javax.inject.Inject

/** 온보딩 마지막 슬라이드의 "시작하기" 시점에 호출. */
class CompleteOnboardingUseCase @Inject constructor(
    private val repository: OnboardingRepository,
) {
    suspend operator fun invoke() = repository.markCompleted()
}
