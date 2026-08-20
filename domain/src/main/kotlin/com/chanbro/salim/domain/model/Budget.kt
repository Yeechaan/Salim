package com.chanbro.salim.domain.model

/**
 * 월 예산 1건. (firestore-schema.md {budget} — PRD 3. 홈 "이번 달 예산")
 * 월별로 다르게 잡을 수 있도록 연/월을 키로 둔다.
 */
data class Budget(
    val year: Int,
    val month: Int,
    val amount: Long, // 원 단위 정수
)
