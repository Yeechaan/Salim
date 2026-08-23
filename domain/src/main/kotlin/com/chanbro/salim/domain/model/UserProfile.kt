package com.chanbro.salim.domain.model

/**
 * 사용자 프로필. (firestore-schema.md users/{userId} — PRD 7. 설정 > 프로필 수정)
 *
 * 여기 입력한 생일/기념일이 디데이 탭의 AUTO 항목으로 반영된다.
 * ddays 컬렉션에 따로 쓰지 않고 이 값에서 파생시키므로, 출처가 하나로 유지된다.
 */
data class UserProfile(
    val birthdayMillis: Long?,      // UTC 자정 millis. null이면 미입력
    val anniversaryMillis: Long?,
)
