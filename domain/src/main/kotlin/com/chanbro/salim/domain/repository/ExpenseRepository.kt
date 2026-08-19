package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * 지출 저장소. 지금은 인메모리 구현, 이후 Firestore 구현으로 교체.
 * (firestore-schema.md: 미연결=users/{uid}, 연결=couples/{coupleId} 이중 경로)
 */
interface ExpenseRepository {
    /** 해당 연월(로컬 기준)의 지출을 최신 입력순으로 관찰. */
    fun observeMonth(year: Int, month: Int): Flow<List<Expense>>

    /** 지출 1건 추가. */
    suspend fun add(expense: Expense)
}
