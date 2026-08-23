package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.UserProfile
import com.chanbro.salim.domain.repository.ProfileRepository
import javax.inject.Inject

/** 프로필 저장. */
class SaveProfileUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(profile: UserProfile) = repository.save(profile)
}
