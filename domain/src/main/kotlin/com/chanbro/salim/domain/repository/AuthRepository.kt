package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * 인증 저장소. (PRD 1. 온보딩 및 로그인 — 1차는 구글 로그인만)
 */
interface AuthRepository {
    /** 로그인/로그아웃에 반응하는 현재 사용자 스트림. 미로그인 시 null. */
    val currentUser: Flow<AuthUser?>

    /**
     * 구글 ID 토큰으로 로그인하고 users/{uid} 문서를 생성/갱신한다.
     * ID 토큰 획득(Credential Manager)은 Activity가 필요해 presentation에서 수행한다.
     */
    suspend fun signInWithGoogle(idToken: String): AuthUser

    /** 로그아웃. (PRD 7. 설정) */
    suspend fun signOut()
}
