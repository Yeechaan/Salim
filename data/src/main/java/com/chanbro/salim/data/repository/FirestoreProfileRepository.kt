package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.UserProfile
import com.chanbro.salim.domain.repository.ProfileRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore 기반 프로필 저장소. (현재 시뮬레이션용 기본 경로 사용)
 * 실제 배포에서는 users/{uid} 문서.
 */
@Singleton
class FirestoreProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ProfileRepository {

    override fun observe(): Flow<UserProfile> = callbackFlow {
        val listener = document().addSnapshotListener { snapshot, error ->
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

    override suspend fun save(profile: UserProfile) {
        // users 문서에는 프로필 외 필드도 함께 살기 때문에 merge로 덮어쓴다.
        val data = mapOf(
            "birthdayMillis" to profile.birthdayMillis,
            "anniversaryMillis" to profile.anniversaryMillis,
        )
        document().set(data, SetOptions.merge()).await()
    }

    private fun document() = firestore.collection("users").document(USER_ID)

    private companion object {
        // TODO: 실제로는 FirebaseAuth.currentUser?.uid (다른 리포지토리와 동일)
        const val USER_ID = "demo"
    }
}
