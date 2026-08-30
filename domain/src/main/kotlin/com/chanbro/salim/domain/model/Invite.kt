package com.chanbro.salim.domain.model

/** 초대 코드 1건. (firestore-schema.md invites/{code}) */
data class Invite(
    val code: String,
    val inviterUid: String,
    val inviterName: String?,
    val expiresAtMillis: Long,
) {
    val inviterNameOrDefault: String
        get() = inviterName?.takeIf { it.isNotBlank() } ?: "상대방"

    fun isExpired(nowMillis: Long): Boolean = expiresAtMillis <= nowMillis
}

/** 코드 조회 결과. 실패 사유가 그대로 화면 문구가 된다 (wireframe/connect.md 상태 분기 종합). */
sealed interface InviteLookup {
    data class Found(val invite: Invite) : InviteLookup
    data object NotFound : InviteLookup
    data object Expired : InviteLookup
    data object OwnCode : InviteLookup
    /** 내가 이미 다른 사람과 연결돼 있다. */
    data object AlreadyConnected : InviteLookup
    data object Failed : InviteLookup
}

/**
 * 연결 성사 결과.
 *
 * [PartnerAlreadyConnected]는 보안 규칙 거절로만 알 수 있다 — 상대의 users 문서를
 * 읽을 수 없어 클라이언트가 미리 확인할 방법이 없다. (docs/wireframe/connect.md)
 */
sealed interface ConnectResult {
    data object Success : ConnectResult
    data object PartnerAlreadyConnected : ConnectResult
    data object Failed : ConnectResult
}
