package com.chanbro.salim.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인한 사용자의 Firestore 경로를 계산한다. (firestore-schema.md "개인/공동 이중 경로 원칙")
 *
 * 현재는 미연결(개인) 경로 `users/{uid}`만 사용한다. 상대방 연결(PRD 9.)이 들어오면
 * 여기서 `couples/{coupleId}`로 분기시키면 되고, 각 저장소는 손대지 않아도 된다.
 */
@Singleton
class UserScope @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    /**
     * 로그인 상태 변화에 반응하는 uid 스트림. 미로그인이면 null.
     *
     * 저장소의 관찰 쿼리는 이 스트림을 flatMapLatest로 받아야 한다. 그래야 로그아웃 시
     * 이전 uid를 보던 Firestore 리스너가 해제되고, 다른 계정으로 로그인하면 새 경로로 재구독된다.
     */
    val uid: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** 쓰기용. 인증 게이트를 통과한 화면에서만 호출되므로 미로그인은 프로그래밍 오류로 본다. */
    fun requireUserDoc(): DocumentReference = userDoc(
        auth.currentUser?.uid ?: error("로그인 상태가 아닌데 사용자 경로를 요청했다"),
    )

    fun userDoc(uid: String): DocumentReference =
        firestore.collection("users").document(uid)
}
