package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.AuthUser
import com.chanbro.salim.domain.repository.AuthRepository
import javax.inject.Inject

/** 구글 ID 토큰으로 로그인. (PRD 1. 카카오 로그인은 추후) */
class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(idToken: String): AuthUser = repository.signInWithGoogle(idToken)
}
