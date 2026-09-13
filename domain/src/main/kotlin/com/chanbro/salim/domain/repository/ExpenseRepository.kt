package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * 지출 저장소.
 * (firestore-schema.md: 미연결=users/{uid}, 연결=couples/{coupleId} 이중 경로)
 */
interface ExpenseRepository {
    /** 해당 연월(로컬 기준)의 지출을 최신 입력순으로 관찰. */
    fun observeMonth(year: Int, month: Int): Flow<List<Expense>>

    /** 단건 조회. 없으면 null (상대가 먼저 지웠을 수 있다). */
    suspend fun get(id: String): Expense?

    /** 지출 1건 추가. */
    suspend fun add(expense: Expense)

    /** 기존 지출 수정. 등록 시각(정렬 키)은 호출부가 원래 값을 그대로 넘긴다. */
    suspend fun update(expense: Expense)

    /** 삭제. */
    suspend fun delete(id: String)
}
