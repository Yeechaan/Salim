package com.chanbro.salim.domain.model

/** 디데이 1건. (firestore-schema.md {ddays}/{ddayId} 대응) */
data class DDay(
    val id: String,
    val title: String,
    val dateMillis: Long,        // 기준 날짜 (UTC 자정 millis)
    val repeatYearly: Boolean,   // 매년 반복 여부
    val source: DDaySource,      // 출처 (자동 반영 / 직접 추가)
    val createdAtMillis: Long,   // 등록 시각
)

/**
 * 디데이 출처. (PRD 6.)
 * AUTO는 설정 > 프로필의 생일/기념일이 자동 반영된 항목으로, 디데이 탭에서 수정·삭제할 수 없다.
 */
enum class DDaySource {
    AUTO,
    MANUAL,
}
