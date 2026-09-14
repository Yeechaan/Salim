package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.Todo
import com.chanbro.salim.domain.model.TodoWritePlan
import kotlinx.coroutines.flow.Flow

/**
 * 할 일 저장소. 상위 항목과 하위 항목을 같은 컬렉션에 평평하게 둔다.
 * (firestore-schema.md: 미연결=users/{uid}, 연결=couples/{coupleId} 이중 경로)
 */
interface TodoRepository {
    /** 상위·하위 항목 전체를 관찰. 묶기·정렬은 호출부가 한다([com.chanbro.salim.domain.model.toSections]). */
    fun observeAll(): Flow<List<Todo>>

    /** 시트 저장 한 번의 추가·수정·삭제를 한 배치로 반영한다. */
    suspend fun apply(plan: TodoWritePlan)

    /** 여러 항목의 완료 여부를 한 배치로 바꾼다. 완료 시각은 완료일 때만 넘긴다. */
    suspend fun setDone(ids: List<String>, done: Boolean, completedAtMillis: Long?)

    /** 여러 항목을 한 배치로 삭제한다. */
    suspend fun delete(ids: List<String>)
}
