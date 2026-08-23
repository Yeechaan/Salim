package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.AuthUser
import com.chanbro.salim.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 로그인 상태를 관찰. 인증 게이트(로그인 화면 vs 홈)의 판단 근거. */
class ObserveAuthStateUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthUser?> = repository.currentUser
}
