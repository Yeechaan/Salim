package com.chanbro.salim.domain.model

/**
 * 가계부 전체보기의 검색어 + 필터 조건. (PRD 4 전체보기, expense.md 4-1)
 *
 * 선택한 달의 지출을 클라이언트에서 거른다 — 쿼리를 새로 걸지 않는다(firestore-schema.md 쿼리 시나리오).
 * 비어 있는 조건은 "거르지 않음"이다.
 */
data class ExpenseFilter(
    /** 메모 부분일치 검색어. 앞뒤 공백·대소문자는 무시한다. */
    val query: String = "",
    /** 기간 시작일 (UTC 자정 millis, 포함). null이면 달의 처음부터. */
    val startDayUtc: Long? = null,
    /** 기간 종료일 (UTC 자정 millis, 그날 끝까지 포함). null이면 달의 끝까지. */
    val endDayUtc: Long? = null,
    /** 고른 카테고리 id. 여러 개면 그중 하나에 맞으면 통과. */
    val categoryIds: Set<String> = emptySet(),
    /** 고른 지출자. 연결 상태에서만 의미가 있다 — 미연결이면 [forConnection]으로 비운다. */
    val spenders: Set<Spender> = emptySet(),
) {
    val hasPeriod: Boolean get() = startDayUtc != null || endDayUtc != null

    /** 시트에서 고르는 조건(기간·카테고리·지출자)이 하나라도 걸려 있는지. 검색어는 제외. */
    val hasConditions: Boolean get() = hasPeriod || categoryIds.isNotEmpty() || spenders.isNotEmpty()

    /** 검색어나 조건이 하나라도 걸려 있는지 — 합계 카드·빈 상태 문구가 이 값으로 갈린다. */
    val isActive: Boolean get() = query.isNotBlank() || hasConditions

    /** 미연결이면 지출자 조건을 버린다 — 모든 지출이 본인 것이라 거를 것이 없다(expense.md 4-1 상태 분기). */
    fun forConnection(connected: Boolean): ExpenseFilter =
        if (connected || spenders.isEmpty()) this else copy(spenders = emptySet())

    /** 시작일이 종료일보다 늦으면 맞바꾼다. */
    fun withPeriod(startDayUtc: Long?, endDayUtc: Long?): ExpenseFilter =
        if (startDayUtc != null && endDayUtc != null && startDayUtc > endDayUtc) {
            copy(startDayUtc = endDayUtc, endDayUtc = startDayUtc)
        } else {
            copy(startDayUtc = startDayUtc, endDayUtc = endDayUtc)
        }

    /** 검색어는 두고 조건만 비운다. */
    fun clearConditions(): ExpenseFilter = ExpenseFilter(query = query)

    /**
     * @param categories 카테고리 목록 — id 없는 예전 지출을 이름으로 찾기 위해 쓴다([findFor]).
     */
    fun matches(expense: Expense, categories: List<Category>): Boolean {
        val keyword = query.trim()
        if (keyword.isNotEmpty() && expense.memo?.contains(keyword, ignoreCase = true) != true) return false
        if (startDayUtc != null && expense.spentAtMillis < startDayUtc) return false
        if (endDayUtc != null && expense.spentAtMillis >= endDayUtc + DAY_MILLIS) return false
        if (spenders.isNotEmpty() && expense.spender !in spenders) return false
        if (categoryIds.isNotEmpty() && categories.findFor(expense)?.id !in categoryIds) return false
        return true
    }

    fun apply(expenses: List<Expense>, categories: List<Category>): List<Expense> =
        if (!isActive) expenses else expenses.filter { matches(it, categories) }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
