package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.UserProfile
import com.chanbro.salim.domain.repository.ProfileRepository
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore 기반 프로필 저장소. 로그인한 사용자의 `users/{uid}` 문서를 쓴다.
 *
 * 이 문서에는 로그인/계정 필드(providers, lastLoginAt 등)가 함께 살기 때문에
 * 프로필 필드만 merge로 덮어쓴다. (firestore-schema.md users 섹션)
 */
@Singleton
class FirestoreProfileRepository @Inject constructor(
    private val userScope: UserScope,
) : ProfileRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<UserProfile> = userScope.uid.flatMapLatest { uid ->
        // 미로그인 상태에서는 빈 프로필 — 디데이 AUTO 항목이 사라진다.
        if (uid == null) {
            return@flatMapLatest flowOf(UserProfile(birthdayMillis = null, anniversaryMillis = null))
        }
        callbackFlow {
            val listener = userScope.userDoc(uid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    UserProfile(
                        birthdayMillis = snapshot?.getLong("birthdayMillis"),
                        anniversaryMillis = snapshot?.getLong("anniversaryMillis"),
                    ),
                )
            }
            awaitClose { listener.remove() }
        }
    }

    override suspend fun save(profile: UserProfile) {
        val data = mapOf(
            "birthdayMillis" to profile.birthdayMillis,
            "anniversaryMillis" to profile.anniversaryMillis,
        )
        userScope.requireUserDoc().set(data, SetOptions.merge()).await()
    }
}
