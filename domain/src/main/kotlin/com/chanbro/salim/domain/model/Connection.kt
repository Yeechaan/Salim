package com.chanbro.salim.domain.model

/**
 * 상대방 연결 상태. (PRD 9. 상대방 연결)
 *
 * 미연결과 "아직 판정 전"을 구분하는 이유 — 판정 전에 [None]으로 보이면
 * 이미 연결된 사용자에게도 "연결해보세요" 배너가 한 번 깜빡인다.
 */
sealed interface Connection {
    /** 커플 여부를 아직 모르는 상태. 연결 관련 UI를 그리지 않는다. */
    data object Unknown : Connection

    /** 미연결. 개인 경로를 쓴다. */
    data object None : Connection

    data class Connected(
        val coupleId: String,
        val partner: PartnerProfile,
        val connectedAtMillis: Long,
    ) : Connection
}

/** 상대방 표시 정보. couples 문서의 members 맵에서 온다 (users 문서는 본인만 읽을 수 있다). */
data class PartnerProfile(
    val uid: String,
    val displayName: String?,
    val photoUrl: String?,
) {
    /** 이름을 못 받은 경우에도 문장이 어색해지지 않도록. */
    val nameOrDefault: String get() = displayName?.takeIf { it.isNotBlank() } ?: "상대방"
}
