package com.chanbro.salim.data.repository

import android.util.Log
import com.chanbro.salim.domain.model.ConnectResult
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.Invite
import com.chanbro.salim.domain.model.InviteCode
import com.chanbro.salim.domain.model.InviteLookup
import com.chanbro.salim.domain.model.PartnerProfile
import com.chanbro.salim.domain.repository.ConnectionRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 상대방 연결. (PRD 9 / firestore-schema.md "상대방 연결")
 *
 * Cloud Functions 없이 클라이언트 쓰기 + 보안 규칙만으로 두 사용자 문서를 동시에 바꾼다.
 * 클라이언트의 사전 검증은 안내용이고, 최종 방어선은 규칙이다.
 */
@Singleton
class FirestoreConnectionRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userScope: UserScope,
) : ConnectionRepository {

    override fun observeConnection(): Flow<Connection> = userScope.coupleState.map { state ->
        val myUid = userScope.currentUid()
        when {
            !state.settled || myUid == null -> Connection.Unknown
            state.couple == null -> Connection.None
            else -> state.couple.toConnected(myUid) ?: Connection.None
        }
    }

    override suspend fun currentInvite(): Invite? {
        val uid = auth.currentUser?.uid ?: return null
        // 내가 발급한 코드는 내 문서에만 적어 둔다 — invites는 list를 막아 뒀기 때문에
        // "내 코드 찾기"를 쿼리로 할 수 없다.
        val code = userScope.userDoc(uid).get().await().getString(FIELD_INVITE_CODE) ?: return null
        val invite = inviteDoc(code).get().await().toInvite() ?: return null
        return invite.takeIf { !it.isExpired(System.currentTimeMillis()) }
    }

    override suspend fun createInvite(): Invite {
        val user = auth.currentUser ?: error("로그인 상태가 아닌데 초대 코드를 만들려 했다")
        val userDoc = userScope.userDoc(user.uid)
        val snapshot = runCatching { userDoc.get().await() }.getOrNull()
        val previousCode = snapshot?.getString(FIELD_INVITE_CODE)

        val now = System.currentTimeMillis()
        val expiresAt = now + INVITE_TTL_MILLIS
        val invite = createUniqueInvite(user.uid, snapshot.displayNameOr(user.displayName), now, expiresAt)

        userDoc.set(mapOf(FIELD_INVITE_CODE to invite.code), SetOptions.merge()).await()
        // 이전 코드 회수. 실패해도 유효기간이 짧아 곧 사라지므로 흐름을 막지 않는다.
        previousCode?.takeIf { it != invite.code }?.let {
            runCatching { inviteDoc(it).delete().await() }
        }
        return invite
    }

    override suspend fun lookupInvite(code: String): InviteLookup {
        val myUid = auth.currentUser?.uid ?: return InviteLookup.Failed
        val normalized = InviteCode.normalize(code)
        val snapshot = try {
            inviteDoc(normalized).get().await()
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "초대 코드 조회 실패", e)
            return InviteLookup.Failed
        }
        val invite = snapshot.toInvite() ?: return InviteLookup.NotFound
        return when {
            invite.inviterUid == myUid -> InviteLookup.OwnCode
            invite.isExpired(System.currentTimeMillis()) -> InviteLookup.Expired
            else -> InviteLookup.Found(invite)
        }
    }

    /**
     * 성사. 네 개의 쓰기를 **하나의 배치**로 커밋한다 (firestore-schema.md "성사 — 단일 WriteBatch").
     * 상대 users 문서 갱신은 규칙이 `getAfter()`로 같은 배치의 커플 문서를 확인해 허용한다.
     */
    override suspend fun connect(invite: Invite): ConnectResult {
        val user = auth.currentUser ?: return ConnectResult.Failed
        val myUid = user.uid
        val partnerUid = invite.inviterUid
        if (myUid == partnerUid) return ConnectResult.Failed

        val memberIds = listOf(myUid, partnerUid).sorted()
        val coupleId = coupleIdOf(myUid, partnerUid)
        val now = System.currentTimeMillis()
        val myName = runCatching { userScope.userDoc(myUid).get().await() }.getOrNull()
            .displayNameOr(user.displayName)

        val batch = firestore.batch()
        batch.set(
            firestore.collection(COLLECTION_COUPLES).document(coupleId),
            mapOf(
                "memberIds" to memberIds,
                // 상대 이름은 초대 문서에서 받아 대신 채워 준다 — users 문서는 본인만 읽을 수 있다.
                "members" to mapOf(
                    myUid to memberEntry(myName, user.photoUrl?.toString(), now),
                    partnerUid to memberEntry(invite.inviterName, null, now),
                ),
                "inviteCode" to invite.code,
                "createdAt" to Timestamp(Date(now)),
                "deletedAt" to null,
            ),
        )
        // update(merge가 아닌)로 쓰는 이유 — 규칙이 "coupleId 한 필드만 바뀌는가"를 본다.
        batch.update(userScope.userDoc(myUid), FIELD_COUPLE_ID, coupleId)
        batch.update(userScope.userDoc(partnerUid), FIELD_COUPLE_ID, coupleId)
        batch.delete(inviteDoc(invite.code))

        return try {
            batch.commit().await()
            ConnectResult.Success
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "연결 성사 배치 거절 (code=${e.code})", e)
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return ConnectResult.Failed
            }
            // 둘이 동시에 서로의 코드를 수락하면 뒤에 커밋한 쪽이 거절된다 — 상대의
            // coupleId가 이미 채워져 있기 때문. 하지만 커플은 이미 만들어졌고 둘 다 멤버라,
            // 여기서 실패라고 말하면 연결됐는데 오류 문구가 뜨는 상태가 된다.
            if (isMemberOf(coupleId)) return ConnectResult.Success
            // 그 밖의 거절은 클라이언트가 확인할 수 없는 사유 하나만 남는다
            // (wireframe/connect.md 상태 분기 종합).
            ConnectResult.PartnerAlreadyConnected
        } catch (e: Exception) {
            Log.w(TAG, "연결 성사 실패", e)
            ConnectResult.Failed
        }
    }

    /** 커플 문서는 멤버만 읽을 수 있어, 읽히면 그 자체로 내가 멤버라는 뜻이다. */
    private suspend fun isMemberOf(coupleId: String): Boolean = runCatching {
        firestore.collection(COLLECTION_COUPLES).document(coupleId).get().await().exists()
    }.getOrDefault(false)

    /**
     * 코드가 겹치면 규칙이 막아 준다 — invites는 `create`만 허용하고 `update`는 막혀 있어서,
     * 이미 있는 코드에 `set`을 하면 거절된다. 따로 존재 여부를 읽지 않아도 된다.
     */
    private suspend fun createUniqueInvite(
        uid: String,
        displayName: String?,
        now: Long,
        expiresAt: Long,
    ): Invite {
        var lastError: Exception? = null
        repeat(CODE_ATTEMPTS) {
            val code = InviteCode.random()
            val data = mapOf(
                "inviterUid" to uid,
                "inviterName" to displayName,
                "createdAt" to Timestamp(Date(now)),
                "expiresAt" to Timestamp(Date(expiresAt)),
            )
            try {
                inviteDoc(code).set(data).await()
                return Invite(code, uid, displayName, expiresAt)
            } catch (e: Exception) {
                // 코드 충돌과 규칙 거절이 똑같이 PERMISSION_DENIED로 온다(update를 막아 뒀기 때문).
                // 화면 문구로는 구분되지 않으니 여기서 남긴다.
                Log.w(TAG, "초대 코드 발급 실패 (${it + 1}/$CODE_ATTEMPTS)", e)
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("초대 코드를 만들지 못했다")
    }

    private fun memberEntry(displayName: String?, photoUrl: String?, nowMillis: Long) = mapOf(
        "displayName" to displayName,
        "photoUrl" to photoUrl,
        "joinedAt" to nowMillis,
    )

    /**
     * 상대에게 보일 내 이름. 설정 > 프로필에서 바꾼 `users/{uid}.displayName`이 우선이다(PRD 7) —
     * 구글 이름을 쓰면 연결 전에 바꾼 이름이 상대 화면에 닿지 않는다.
     * 문서를 못 읽었거나 이름이 비어 있으면 구글 이름으로 떨어진다.
     */
    private fun DocumentSnapshot?.displayNameOr(fallback: String?): String? =
        this?.getString("displayName")?.takeIf { it.isNotBlank() } ?: fallback

    private fun inviteDoc(code: String) =
        firestore.collection(COLLECTION_INVITES).document(code)

    private fun DocumentSnapshot.toInvite(): Invite? {
        val inviterUid = getString("inviterUid") ?: return null
        val expiresAt = getTimestamp("expiresAt")?.toDate()?.time ?: return null
        return Invite(
            code = id,
            inviterUid = inviterUid,
            inviterName = getString("inviterName"),
            expiresAtMillis = expiresAt,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toConnected(myUid: String): Connection.Connected? {
        val partnerUid = memberIds().firstOrNull { it != myUid } ?: return null
        val member = (get("members") as? Map<String, Any?>)?.get(partnerUid) as? Map<String, Any?>
        return Connection.Connected(
            coupleId = id,
            partner = PartnerProfile(
                uid = partnerUid,
                displayName = member?.get("displayName") as? String,
                photoUrl = member?.get("photoUrl") as? String,
            ),
            connectedAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        )
    }

    private companion object {
        const val TAG = "SalimConnect"
        const val COLLECTION_INVITES = "invites"
        const val COLLECTION_COUPLES = "couples"
        const val FIELD_INVITE_CODE = "inviteCode"
        const val FIELD_COUPLE_ID = "coupleId"
        const val CODE_ATTEMPTS = 5
        val INVITE_TTL_MILLIS = 30 * 60 * 1000L // 30분 (PRD 9)
    }
}

/**
 * 커플 문서 id는 두 uid로부터 결정적으로 만든다 — 같은 두 사람에 대한 문서가 항상 하나라
 * 경합으로 커플이 둘 생기지 않고, 보안 규칙이 id와 memberIds의 일치를 검증할 수 있다.
 */
fun coupleIdOf(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"
