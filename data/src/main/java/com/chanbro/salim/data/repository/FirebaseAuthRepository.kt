package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.AuthUser
import com.chanbro.salim.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication 기반 인증 저장소. (PRD 1. 온보딩 및 로그인)
 *
 * 크리덴셜의 SSOT는 Firebase Auth이고, Firestore `users/{uid}` 문서에는
 * 프로필/연동 상태만 미러링한다. (firestore-schema.md users 섹션)
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential).await().user
            ?: error("로그인은 성공했으나 사용자 정보를 받지 못했다")
        upsertUserDocument(user)
        return user.toAuthUser()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    /**
     * 최초 가입과 재로그인을 한 번에 처리한다.
     * 최초에만 createdAt/providers/primaryProvider를 심고, 재로그인은 lastLoginAt과
     * 소셜 프로필만 갱신한다 — 두 경우가 같은 트랜잭션 안에서 갈린다.
     */
    private suspend fun upsertUserDocument(user: FirebaseUser) {
        val doc = firestore.collection("users").document(user.uid)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(doc)
            val profile = mapOf(
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl?.toString(),
                "lastLoginAt" to FieldValue.serverTimestamp(),
            )
            if (snapshot.exists()) {
                transaction.update(doc, profile)
            } else {
                transaction.set(
                    doc,
                    profile + mapOf(
                        "providers" to listOf(PROVIDER_GOOGLE),
                        "primaryProvider" to PROVIDER_GOOGLE,
                        "createdAt" to FieldValue.serverTimestamp(),
                        // 상대방 연결 전까지는 개인 경로를 쓴다 (PRD 1.)
                        "coupleId" to null,
                        "deletedAt" to null,
                    ),
                )
            }
        }.await()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
    )

    private companion object {
        const val PROVIDER_GOOGLE = "google"
    }
}
