package com.chanbro.salim.domain.model

/**
 * 로그인한 사용자. (firestore-schema.md users/{userId} 중 인증에서 오는 값만)
 *
 * 크리덴셜의 SSOT는 Firebase Authentication이고, Firestore users 문서에는
 * 프로필만 미러링한다 — 이 모델도 미러링 대상 필드만 담는다.
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)
