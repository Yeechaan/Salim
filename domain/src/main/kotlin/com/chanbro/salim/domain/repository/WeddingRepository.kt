package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.WeddingExpense
import com.chanbro.salim.domain.model.WeddingSettings
import com.chanbro.salim.domain.model.WeddingTask
import com.chanbro.salim.domain.model.WeddingVendor
import kotlinx.coroutines.flow.Flow

/**
 * 결혼 준비 저장소. (firestore-schema.md "결혼 준비")
 * 경로는 다른 공유 데이터와 같다 — 미연결=users/{uid}, 연결=couples/{coupleId}.
 */
interface WeddingRepository {
    /**
     * 설정을 관찰. 문서가 없으면 기본값(꺼짐).
     * 켜져 있는데 기본 항목을 아직 안 채운 것을 서버에서 확인하면 이 구독이 기본 항목을 채운다.
     */
    fun observeSettings(): Flow<WeddingSettings>

    suspend fun setEnabled(enabled: Boolean)

    /** null이면 미정으로 되돌린다. */
    suspend fun setWeddingDate(dateMillis: Long?)

    suspend fun setTotalBudget(amount: Long)

    fun observeTasks(): Flow<List<WeddingTask>>

    /** 새 항목이면 만들고, 있던 항목이면 제목·시기·담당자만 고친다(완료 여부는 그대로). */
    suspend fun saveTask(task: WeddingTask, isNew: Boolean)

    suspend fun setTaskDone(id: String, done: Boolean, completedAtMillis: Long?)

    suspend fun deleteTask(id: String)

    fun observeExpenses(): Flow<List<WeddingExpense>>

    /** 추가·수정 모두 문서를 통째로 쓴다. 수정할 때 등록 시각은 호출부가 원래 값으로 넘긴다. */
    suspend fun saveExpense(expense: WeddingExpense, isNew: Boolean)

    suspend fun deleteExpense(id: String)

    fun observeVendors(): Flow<List<WeddingVendor>>

    suspend fun saveVendor(vendor: WeddingVendor, isNew: Boolean)

    /** 업체를 지우고, 그 업체를 가리키던 지출의 업체 표시를 한 배치로 뗀다. */
    suspend fun deleteVendor(id: String, linkedExpenseIds: List<String>)
}
