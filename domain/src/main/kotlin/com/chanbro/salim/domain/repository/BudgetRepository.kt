package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.Budget
import kotlinx.coroutines.flow.Flow

/**
 * 월 예산 저장소.
 * (firestore-schema.md: 미연결=users/{uid}, 연결=couples/{coupleId} 이중 경로)
 */
interface BudgetRepository {
    /** 해당 연월의 예산을 관찰. 설정 전이면 null. */
    fun observe(year: Int, month: Int): Flow<Budget?>

    /** 해당 연월 예산 설정(덮어씀). */
    suspend fun save(budget: Budget)
}
