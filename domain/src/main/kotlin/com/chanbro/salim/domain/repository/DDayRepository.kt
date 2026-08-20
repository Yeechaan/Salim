package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.DDay
import kotlinx.coroutines.flow.Flow

/**
 * 디데이 저장소.
 * (firestore-schema.md: 미연결=users/{uid}, 연결=couples/{coupleId} 이중 경로)
 */
interface DDayRepository {
    /** 전체 디데이를 관찰. 정렬(가까운 순)은 표시 시점에 계산한다. */
    fun observeAll(): Flow<List<DDay>>

    /** 단건 조회. 없으면 null. */
    suspend fun get(id: String): DDay?

    /** 신규 등록과 수정을 겸한다(같은 id면 덮어씀). */
    suspend fun save(dDay: DDay)

    /** 삭제. 직접 추가한 항목만 호출한다 (PRD 6.). */
    suspend fun delete(id: String)
}
