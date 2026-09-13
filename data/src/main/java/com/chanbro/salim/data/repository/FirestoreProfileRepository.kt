package com.chanbro.salim.data.repository

import android.util.Log
import com.chanbro.salim.domain.model.UserProfile
import com.chanbro.salim.domain.repository.ProfileRepository
import com.google.firebase.firestore.FieldPath
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
 *
 * **다른 저장소와 달리 연결 후에도 공동 경로로 옮기지 않는다** — 생일/기념일은
 * 각자의 개인 정보라 커플 문서에 두지 않기로 했다. (firestore-schema.md 이중 경로 원칙의 예외)
 * 표시 이름만 예외의 예외로, 상대가 볼 수 있도록 커플 문서에 사본을 남긴다 ([mirrorNameToCouple]).
 */
@Singleton
class FirestoreProfileRepository @Inject constructor(
    private val userScope: UserScope,
) : ProfileRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<UserProfile> = userScope.uid.flatMapLatest { uid ->
        // 미로그인 상태에서는 빈 프로필 — 디데이 AUTO 항목이 사라진다.
        if (uid == null) {
            return@flatMapLatest flowOf(
                UserProfile(displayName = null, birthdayMillis = null, anniversaryMillis = null),
            )
        }
        callbackFlow {
            val listener = userScope.userDoc(uid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    UserProfile(
                        displayName = snapshot?.getString("displayName"),
                        birthdayMillis = snapshot?.getLong("birthdayMillis"),
                        anniversaryMillis = snapshot?.getLong("anniversaryMillis"),
                    ),
                )
            }
            awaitClose { listener.remove() }
        }
    }

    override suspend fun save(profile: UserProfile) {
        val name = profile.displayName?.trim()?.ifBlank { null }
        val data = mapOf(
            "displayName" to name,
            "birthdayMillis" to profile.birthdayMillis,
            "anniversaryMillis" to profile.anniversaryMillis,
        )
        val uid = userScope.currentUid() ?: error("로그인 상태가 아닌데 프로필을 저장하려 했다")
        userScope.userDoc(uid).set(data, SetOptions.merge()).await()
        mirrorNameToCouple(uid, name)
    }

    /**
     * 바꾼 이름을 커플 문서의 members 맵에도 반영한다. 상대는 내 users 문서를 읽을 수 없어서,
     * 상대 화면(지출자 이름 등)에 내 이름이 닿는 경로가 이 사본뿐이다. (firestore-schema.md couples.members)
     *
     * 실패해도 내 프로필 저장은 이미 끝난 뒤다 — 다음 저장에서 다시 시도되므로 흐름을 막지 않는다.
     */
    private suspend fun mirrorNameToCouple(uid: String, name: String?) {
        // partnerUid가 있다는 것은 scope.doc이 커플 문서라는 뜻이다. 미연결이면 반영할 곳이 없다.
        val scope = userScope.requireScope().takeIf { it.partnerUid != null } ?: return
        runCatching {
            scope.doc.update(FieldPath.of("members", uid, "displayName"), name).await()
        }.onFailure { Log.w(TAG, "커플 문서의 표시 이름 갱신 실패", it) }
    }

    private companion object {
        const val TAG = "SalimProfile"
    }
}
