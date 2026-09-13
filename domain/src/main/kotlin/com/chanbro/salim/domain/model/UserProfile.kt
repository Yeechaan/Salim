package com.chanbro.salim.domain.model

/**
 * 사용자 프로필. (firestore-schema.md users/{userId} — PRD 7. 설정 > 프로필 수정)
 *
 * 여기 입력한 생일/기념일이 디데이 탭의 AUTO 항목으로 반영된다.
 * ddays 컬렉션에 따로 쓰지 않고 이 값에서 파생시키므로, 출처가 하나로 유지된다.
 */
data class UserProfile(
    /** 표시 이름. 최초값은 구글 프로필 이름이고, 설정에서 바꿀 수 있다. 비우면 null. */
    val displayName: String?,
    val birthdayMillis: Long?,      // UTC 자정 millis. null이면 미입력
    val anniversaryMillis: Long?,
) {
    /** 이름을 비워 둔 사용자에게도 문구가 어색해지지 않도록. */
    val nameOrDefault: String get() = displayName?.takeIf { it.isNotBlank() } ?: Spender.ME.label
}
