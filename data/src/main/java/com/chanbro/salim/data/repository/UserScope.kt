package com.chanbro.salim.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인한 사용자의 데이터 경로. (firestore-schema.md "개인/공동 이중 경로 원칙")
 *
 * - 미연결: `users/{uid}`
 * - 연결: `couples/{coupleId}`
 *
 * **연결 여부 판정의 SSOT는 `couples` 쿼리다.** `users/{uid}.coupleId` 필드를 쓰지 않는 이유는
 * 상대가 내 문서를 쓸 수 있는 창구를 최소로 유지하기 위해서다 — 성사 배치의 일부가 유실돼
 * 그 필드가 비어도 쿼리는 커플을 찾아낸다.
 */
@Singleton
class UserScope @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    /**
     * 앱 수명 전체에 걸친 구독. 경로는 화면이 보고 있지 않을 때도 최신이어야 한다 —
     * 쓰기(`requireScope`)가 구독 여부와 무관하게 올바른 경로를 집어야 하기 때문.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 로그인 상태 변화에 반응하는 uid 스트림. 미로그인이면 null. */
    val uid: StateFlow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(appScope, SharingStarted.Eagerly, auth.currentUser?.uid)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val resolved: StateFlow<Resolved> = uid.flatMapLatest { uid ->
        if (uid == null) flowOf(Resolved.loggedOut()) else coupleFlow(uid).map { it.toResolved(uid) }
    }.stateIn(appScope, SharingStarted.Eagerly, Resolved.pending())

    /**
     * 읽기용 경로. null이면 미로그인이거나 아직 커플 여부를 판정하지 못한 상태다.
     * 관찰 쿼리는 이 스트림을 flatMapLatest로 받아야 로그아웃·연결 시 리스너가 갈아탄다.
     *
     * distinctUntilChanged가 필요한 이유 — 커플 문서의 members 맵만 바뀌어도 스냅샷은 새로 오는데,
     * 경로가 그대로면 저장소들이 리스너를 헐고 다시 붙을 이유가 없다.
     */
    val scope: Flow<DataScope?> = resolved.map { it.scope }.distinctUntilChanged()

    /** 커플 문서. 미연결이면 null, 판정 전에는 [CoupleState.settled]가 false. */
    val coupleState: Flow<CoupleState> = resolved.map { CoupleState(it.settled, it.couple) }

    /**
     * 쓰기용. 경로가 정해질 때까지 기다린다 — 앱 시작 직후 커플 판정이 끝나기 전에 쓰면
     * 연결된 사용자의 기록이 개인 경로로 새기 때문에, 잘못된 경로보다 잠깐 기다리는 쪽을 택했다.
     * 호출부는 모두 인증 게이트를 지난 화면이라 미로그인은 프로그래밍 오류로 본다.
     */
    suspend fun requireScope(): DataScope = scope.filterNotNull().first()

    fun userDoc(uid: String): DocumentReference =
        firestore.collection("users").document(uid)

    fun currentUid(): String? = auth.currentUser?.uid

    /**
     * 내가 속한 커플 문서를 구독한다. 규칙이 `memberIds`에 내가 있는 문서만 허용하므로
     * array-contains 쿼리가 그대로 통과한다.
     */
    private fun coupleFlow(uid: String): Flow<DocumentSnapshot?> = callbackFlow {
        val listener = firestore.collection("couples")
            .whereArrayContains("memberIds", uid)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // 커플을 못 읽으면 미연결로 본다. 여기서 스트림을 닫으면 앱의 모든 데이터가
                    // 함께 죽기 때문에, 조회 실패는 "커플 없음"과 같게 다룬다.
                    trySend(null)
                    return@addSnapshotListener
                }
                val doc = snapshot?.documents?.firstOrNull()
                // deletedAt이 있으면 유예기간 중이라 공동 경로를 쓰지 않는다 (PRD 9).
                trySend(doc?.takeIf { it.getTimestamp("deletedAt") == null })
            }
        awaitClose { listener.remove() }
    }

    private fun DocumentSnapshot?.toResolved(uid: String): Resolved {
        val couple = this ?: return Resolved(settled = true, couple = null, scope = DataScope(userDoc(uid), uid, null))
        val partnerUid = couple.memberIds().firstOrNull { it != uid }
        return Resolved(
            settled = true,
            couple = couple,
            scope = DataScope(couple.reference, uid, partnerUid),
        )
    }

    private data class Resolved(
        /** 커플 여부 판정이 끝났는가. 미로그인도 "끝난" 상태로 본다. */
        val settled: Boolean,
        val couple: DocumentSnapshot?,
        val scope: DataScope?,
    ) {
        companion object {
            fun pending() = Resolved(settled = false, couple = null, scope = null)
            fun loggedOut() = Resolved(settled = true, couple = null, scope = null)
        }
    }
}

/** 저장소가 쓰기·읽기에 필요한 것 전부. 지출자/일정 주인을 uid로 저장하느라 uid도 함께 넘긴다. */
data class DataScope(
    val doc: DocumentReference,
    val myUid: String,
    /** 미연결이면 null. */
    val partnerUid: String?,
)

data class CoupleState(
    val settled: Boolean,
    val couple: DocumentSnapshot?,
)

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.memberIds(): List<String> =
    (get("memberIds") as? List<String>).orEmpty()
