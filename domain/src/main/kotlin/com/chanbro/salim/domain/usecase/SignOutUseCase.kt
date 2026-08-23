package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.repository.AuthRepository
import javax.inject.Inject

/** 로그아웃. (PRD 7. 설정) */
class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.signOut()
}
