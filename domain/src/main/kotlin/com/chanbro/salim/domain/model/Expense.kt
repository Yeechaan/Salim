package com.chanbro.salim.domain.model

/** 지출 1건. (firestore-schema.md {expenses}/{expenseId} 대응 — 인메모리 단계) */
data class Expense(
    val id: String,
    val amount: Long,          // 원 단위 정수
    val spentAtMillis: Long,   // 지출 일시 (UTC 기준 millis: 날짜+시간 합산)
    val spender: Spender,      // 지출자
    val categoryName: String,  // 저장 시점의 카테고리명 — id가 없거나 카테고리를 못 찾을 때 표시용
    val memo: String?,         // 메모 (리스트 상호명/라벨로도 사용)
    val createdAtMillis: Long, // 등록 시각 — 리스트 정렬 키(입력 순서)
    /** 카테고리 문서 id. 표시는 이 id로 지금 이름을 찾는다. 카테고리 기능 이전 지출은 null. */
    val categoryId: String? = null,
)

/** 지출자: 나 / 배우자. 미연결 상태에서는 항상 ME. */
enum class Spender(val label: String) {
    ME("나"),
    PARTNER("배우자"),
}
