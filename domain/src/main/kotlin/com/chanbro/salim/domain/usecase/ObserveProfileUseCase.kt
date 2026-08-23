package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.UserProfile
import com.chanbro.salim.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 프로필을 관찰. */
class ObserveProfileUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    operator fun invoke(): Flow<UserProfile> = repository.observe()
}
